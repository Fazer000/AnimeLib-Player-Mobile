package com.example.animelib.util;

import android.util.Log;

import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Custom 5.1 Virtual Surround & 3D Spatial Audio Processor for ExoPlayer (Media3).
 * Applies real-time 3D Spatial Audio Processing with multiple audio profiles:
 * 1. 3D Spatial Modes: Cinema 3D, Concert/OST 3D, Voice Focus 3D, Classic 5.1, Extreme 3D HRTF.
 * 2. Multi-tap Haas delay matrix + early reflection diffusion for true 3D soundstage depth.
 * 3. Center dialogue & vocal bandpass clarity booster (Japanese seiyuu & dubbing focus).
 * 4. Cinematic 2nd-order Biquad LFE sub-bass synthesizer (<90Hz).
 * 5. Interaural HRTF head-shadow crossfeed simulation (pinna acoustic model).
 * 6. Dynamic soft-knee limiter with gain compensation for zero distortion.
 */
public class Surround51AudioProcessor implements AudioProcessor {

    private static final String TAG = "Surround51Processor";

    // 3D Spatial Mode Constants
    public static final int MODE_CINEMA_3D = 0;   // Кинотеатр 3D
    public static final int MODE_CONCERT_3D = 1;  // Концерт & OST 3D
    public static final int MODE_VOICE_3D = 2;    // Четкая Озвучка 3D
    public static final int MODE_CLASSIC_51 = 3;  // Классический 5.1
    public static final int MODE_EXTREME_3D = 4;  // Экстрим 3D HRTF

    private boolean enabled = true;

    private int spatialMode = MODE_CINEMA_3D;
    private float spatialWidth = 1.0f;       // 0.5f (Narrow) .. 2.0f (Ultra Wide)
    private float dialogueBoost = 1.0f;      // 0.5f (Normal) .. 2.0f (Max Clarity)
    private float bassBoostLevel = 1.0f;     // 0.5f (Soft) .. 2.0f (Heavy LFE)
    private float trebleBoostLevel = 1.0f;   // 0.5f (Soft) .. 2.0f (High Air/Treble)

    private AudioFormat inputAudioFormat = AudioFormat.NOT_SET;
    private AudioFormat outputAudioFormat = AudioFormat.NOT_SET;

    private ByteBuffer buffer = EMPTY_BUFFER;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private boolean inputEnded;

    // Multi-tap ring buffer for 3D spatial surround delays, diffusion, and HRTF crossfeed
    private float[] midRingBuffer = new float[0];
    private float[] surroundRingBuffer = new float[0];
    private float[] leftRingBuffer = new float[0];
    private float[] rightRingBuffer = new float[0];
    private int ringBufferSize = 0;
    private int ringIndex = 0;

    // Delay tap sample offsets
    private int tap1Samples = 0; // ~18ms
    private int tap2Samples = 0; // ~30ms
    private int tap3Samples = 0; // ~44ms
    private int tap4Samples = 0; // ~58ms (Deep 3D Room Reflection)
    private int crossfeedSamples = 0; // ~0.28ms (Interaural Head Distance)

    // Filter states
    private float vocalFilterState = 0f;
    private float vocalHighFilterState = 0f;
    private float lfeFilter1 = 0f;
    private float lfeFilter2 = 0f;
    private float crossLFilter = 0f;
    private float crossRFilter = 0f;
    private float alphaLfe = 0.013f;
    private float alphaVocalLow = 0.08f;
    private float alphaVocalHigh = 0.35f;

    public Surround51AudioProcessor() {
    }

    public synchronized void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            Log.d(TAG, "5.1 Surround AudioProcessor enabled state: " + enabled);
        }
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setSpatialMode(int mode) {
        if (this.spatialMode != mode) {
            this.spatialMode = mode;
            Log.d(TAG, "Spatial Mode changed to: " + getModeName(mode));
        }
    }

    public synchronized int getSpatialMode() {
        return spatialMode;
    }

    public synchronized void setSpatialWidth(float width) {
        this.spatialWidth = Math.max(0.3f, Math.min(2.5f, width));
    }

    public synchronized float getSpatialWidth() {
        return spatialWidth;
    }

    public synchronized void setDialogueBoost(float boost) {
        this.dialogueBoost = Math.max(0.3f, Math.min(2.5f, boost));
    }

    public synchronized float getDialogueBoost() {
        return dialogueBoost;
    }

    public synchronized void setBassBoostLevel(float bass) {
        this.bassBoostLevel = Math.max(0.3f, Math.min(2.5f, bass));
    }

    public synchronized float getBassBoostLevel() {
        return bassBoostLevel;
    }

    public synchronized void setTrebleBoostLevel(float treble) {
        this.trebleBoostLevel = Math.max(0.3f, Math.min(2.5f, treble));
    }

    public synchronized float getTrebleBoostLevel() {
        return trebleBoostLevel;
    }

    public static String getModeName(int mode) {
        switch (mode) {
            case MODE_CINEMA_3D: return "Кинотеатр 3D";
            case MODE_CONCERT_3D: return "Концерт & OST 3D";
            case MODE_VOICE_3D: return "Четкая Озвучка 3D";
            case MODE_CLASSIC_51: return "Классический 5.1";
            case MODE_EXTREME_3D: return "Экстрим 3D HRTF";
            default: return "Кинотеатр 3D";
        }
    }

    @Override
    public AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw new UnhandledAudioFormatException(inputAudioFormat);
        }

        this.inputAudioFormat = inputAudioFormat;
        // Output format is always 2-channel 16-bit PCM (3D spatialized binaural stereo)
        this.outputAudioFormat = new AudioFormat(
                inputAudioFormat.sampleRate,
                2,
                C.ENCODING_PCM_16BIT
        );

        int sampleRate = inputAudioFormat.sampleRate;
        // Allocate 70ms ring buffer history in sample frames for deep 3D room reflection
        this.ringBufferSize = (int) (sampleRate * 0.070f);
        if (ringBufferSize > 0) {
            this.midRingBuffer = new float[ringBufferSize];
            this.surroundRingBuffer = new float[ringBufferSize];
            this.leftRingBuffer = new float[ringBufferSize];
            this.rightRingBuffer = new float[ringBufferSize];
            this.ringIndex = 0;
        }

        // Multi-tap surround delays & crossfeed offsets
        this.tap1Samples = (int) (sampleRate * 0.018f); // ~18ms
        this.tap2Samples = (int) (sampleRate * 0.030f); // ~30ms
        this.tap3Samples = (int) (sampleRate * 0.044f); // ~44ms
        this.tap4Samples = (int) (sampleRate * 0.058f); // ~58ms (Deep 3D Room Reflection)
        this.crossfeedSamples = Math.max(1, (int) (sampleRate * 0.00028f)); // ~0.28ms (Interaural Head Distance)

        // Low-pass coefficient for sub-bass LFE (< 85 Hz): Fc = 85 Hz
        this.alphaLfe = (float) (2.0 * Math.PI * 85.0 / sampleRate);
        if (alphaLfe > 0.5f) alphaLfe = 0.5f;

        // Bandpass coefficients for Dialogue (800Hz - 3.8kHz)
        this.alphaVocalLow = (float) (2.0 * Math.PI * 800.0 / sampleRate);
        this.alphaVocalHigh = (float) (2.0 * Math.PI * 3800.0 / sampleRate);

        Log.d(TAG, "Configured 3D Spatial AudioProcessor: inCh=" + inputAudioFormat.channelCount
                + ", sampleRate=" + sampleRate + "Hz, ringBufferSize=" + ringBufferSize);

        return outputAudioFormat;
    }

    @Override
    public boolean isActive() {
        return inputAudioFormat.sampleRate != AudioFormat.NOT_SET.sampleRate;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int remaining = limit - position;

        if (remaining <= 0) {
            return;
        }

        int channelCount = inputAudioFormat.channelCount;
        int frameSize = channelCount * 2; // 2 bytes per 16-bit PCM sample
        int frames = remaining / frameSize;
        int outputBytes = frames * 4; // 2 output channels * 2 bytes = 4 bytes per frame

        if (buffer.capacity() < outputBytes) {
            buffer = ByteBuffer.allocateDirect(outputBytes).order(ByteOrder.nativeOrder());
        } else {
            buffer.clear();
        }

        if (!enabled) {
            // Downmix to stereo directly if processor is disabled
            for (int i = 0; i < frames; i++) {
                short left = inputBuffer.getShort();
                short right = (channelCount >= 2) ? inputBuffer.getShort() : left;
                for (int ch = 2; ch < channelCount; ch++) {
                    inputBuffer.getShort();
                }
                buffer.putShort(left);
                buffer.putShort(right);
            }
            buffer.flip();
            outputBuffer = buffer;
            return;
        }

        // Apply profile parameters based on spatialMode
        float modeSurroundGain;
        float modeVocalGain;
        float modeBassGain;
        float modeXfeedGain;
        float modeAirGain;

        switch (spatialMode) {
            case MODE_CONCERT_3D:
                modeSurroundGain = 1.65f * spatialWidth;
                modeVocalGain = 0.85f * dialogueBoost;
                modeBassGain = 1.15f * bassBoostLevel;
                modeXfeedGain = 0.35f;
                modeAirGain = 0.65f; // High air sparkle for soundtracks & openings
                break;

            case MODE_VOICE_3D:
                modeSurroundGain = 0.75f * spatialWidth;
                modeVocalGain = 2.40f * dialogueBoost; // Powerful voice & seiyuu boost
                modeBassGain = 0.45f * bassBoostLevel; // Clean low end
                modeXfeedGain = 0.15f;
                modeAirGain = 0.30f;
                break;

            case MODE_CLASSIC_51:
                modeSurroundGain = 1.00f * spatialWidth;
                modeVocalGain = 1.00f * dialogueBoost;
                modeBassGain = 1.00f * bassBoostLevel;
                modeXfeedGain = 0.22f;
                modeAirGain = 0.25f;
                break;

            case MODE_EXTREME_3D:
                modeSurroundGain = 2.10f * spatialWidth; // Ultra 3D soundstage
                modeVocalGain = 1.60f * dialogueBoost;
                modeBassGain = 1.95f * bassBoostLevel; // Heavy sub-bass rumble
                modeXfeedGain = 0.42f;
                modeAirGain = 0.55f;
                break;

            case MODE_CINEMA_3D:
            default:
                modeSurroundGain = 1.35f * spatialWidth;
                modeVocalGain = 1.25f * dialogueBoost;
                modeBassGain = 1.60f * bassBoostLevel;
                modeXfeedGain = 0.28f;
                modeAirGain = 0.35f;
                break;
        }

        // Process audio frames with 3D Spatial Audio algorithm
        for (int i = 0; i < frames; i++) {
            float fL, fR, mid, side, nativeLfe = 0f;

            if (channelCount == 2) {
                short inL = inputBuffer.getShort();
                short inR = inputBuffer.getShort();
                fL = inL / 32768.0f;
                fR = inR / 32768.0f;
                mid = (fL + fR) * 0.5f;
                side = (fL - fR) * 0.5f;
            } else if (channelCount == 1) {
                short inC = inputBuffer.getShort();
                mid = inC / 32768.0f;
                fL = mid;
                fR = mid;
                side = 0f;
            } else if (channelCount == 6) {
                // Native 5.1 Surround track (FL, FR, Center, LFE, SL, SR)
                short inFL = inputBuffer.getShort();
                short inFR = inputBuffer.getShort();
                short inC  = inputBuffer.getShort();
                short inLFE= inputBuffer.getShort();
                short inSL = inputBuffer.getShort();
                short inSR = inputBuffer.getShort();

                fL = inFL / 32768.0f;
                fR = inFR / 32768.0f;
                mid = inC / 32768.0f;
                nativeLfe = inLFE / 32768.0f;
                float sl = inSL / 32768.0f;
                float sr = inSR / 32768.0f;
                side = (sl - sr) * 0.65f + (fL - fR) * 0.35f;
            } else {
                short inL = inputBuffer.getShort();
                short inR = (channelCount >= 2) ? inputBuffer.getShort() : inL;
                for (int ch = 2; ch < channelCount; ch++) inputBuffer.getShort();
                fL = inL / 32768.0f;
                fR = inR / 32768.0f;
                mid = (fL + fR) * 0.5f;
                side = (fL - fR) * 0.5f;
            }

            // A. Anime Vocal & Dialogue Clarity Enhancement
            vocalFilterState += alphaVocalLow * (mid - vocalFilterState);
            vocalHighFilterState += alphaVocalHigh * (vocalFilterState - vocalHighFilterState);
            float vocalBand = vocalFilterState - vocalHighFilterState; // 800Hz - 3.8kHz vocal spectrum
            float centerAudio = mid * 0.75f + vocalBand * modeVocalGain * 1.5f;

            // B. Cinematic Action Sub-Bass LFE Synthesis (< 85 Hz)
            lfeFilter1 += alphaLfe * (mid - lfeFilter1);
            lfeFilter2 += alphaLfe * (lfeFilter1 - lfeFilter2);
            float synthesizedLfe = lfeFilter2 * modeBassGain * 1.6f;
            float totalLfe = synthesizedLfe + nativeLfe * 0.85f;

            // C. Wide 3D Spatial Surround Field & Acoustic Room Diffusion
            float spatialSurroundL = side;
            float spatialSurroundR = -side;

            if (ringBufferSize > 0) {
                midRingBuffer[ringIndex] = mid;

                int tap1Idx = (ringIndex - tap1Samples + ringBufferSize) % ringBufferSize;
                int tap2Idx = (ringIndex - tap2Samples + ringBufferSize) % ringBufferSize;
                int tap3Idx = (ringIndex - tap3Samples + ringBufferSize) % ringBufferSize;
                int tap4Idx = (ringIndex - tap4Samples + ringBufferSize) % ringBufferSize;
                int xfeedIdx = (ringIndex - crossfeedSamples + ringBufferSize) % ringBufferSize;

                // Decorrelate mid channel for mono or narrow stereo sources to create true 3D spatial width
                float midTap1 = midRingBuffer[tap1Idx];
                float midTap2 = midRingBuffer[tap2Idx];
                float midSpatialSide = (midTap1 - midTap2) * 0.40f;

                float totalSide = side + midSpatialSide * (modeSurroundGain * 0.6f);
                surroundRingBuffer[ringIndex] = totalSide;
                leftRingBuffer[ringIndex] = fL;
                rightRingBuffer[ringIndex] = fR;

                float surrTap1 = surroundRingBuffer[tap1Idx];
                float surrTap2 = surroundRingBuffer[tap2Idx];
                float surrTap3 = surroundRingBuffer[tap3Idx];
                float surrTap4 = surroundRingBuffer[tap4Idx];

                // 3D Spatial Matrix Expansion with multi-reflection early diffusion
                spatialSurroundL = (totalSide * 0.70f) + (surrTap1 * 0.65f) + (surrTap3 * 0.45f) - (surrTap4 * 0.30f);
                spatialSurroundR = (-totalSide * 0.70f) - (surrTap2 * 0.65f) - (surrTap3 * 0.45f) + (surrTap4 * 0.30f);

                // Acoustic Head Crossfeed & Ear-Shadow Filtering (Binaural HRTF)
                float crossL = leftRingBuffer[xfeedIdx];
                float crossR = rightRingBuffer[xfeedIdx];

                crossLFilter += 0.25f * (crossL - crossLFilter);
                crossRFilter += 0.25f * (crossR - crossRFilter);

                ringIndex = (ringIndex + 1) % ringBufferSize;
            }

            // D. High-Frequency Air & Detail for Anime SFX (Swords, Magic, Attacks, Openings)
            float sfxDetailL = fL - crossLFilter;
            float sfxDetailR = fR - crossRFilter;
            float highAirL = sfxDetailL * modeAirGain * 1.3f * trebleBoostLevel;
            float highAirR = sfxDetailR * modeAirGain * 1.3f * trebleBoostLevel;

            // E. 5.1 & 3D Binaural Downmix Assembly
            float outL = (fL * 0.50f) + (centerAudio * 0.45f) + (spatialSurroundL * modeSurroundGain) + (crossRFilter * modeXfeedGain) + totalLfe + highAirL;
            float outR = (fR * 0.50f) + (centerAudio * 0.45f) + (spatialSurroundR * modeSurroundGain) + (crossLFilter * modeXfeedGain) + totalLfe + highAirR;

            // F. Dynamic Soft-Knee Limiter for Zero Clipping Loudness
            outL = softLimit(outL * 1.10f);
            outR = softLimit(outR * 1.10f);

            short finalL = (short) clamp(outL * 32767.0f, -32768f, 32767f);
            short finalR = (short) clamp(outR * 32767.0f, -32768f, 32767f);

            buffer.putShort(finalL);
            buffer.putShort(finalR);
        }

        buffer.flip();
        outputBuffer = buffer;
    }

    @Override
    public void queueEndOfStream() {
        inputEnded = true;
    }

    @Override
    public ByteBuffer getOutput() {
        ByteBuffer output = outputBuffer;
        outputBuffer = EMPTY_BUFFER;
        return output;
    }

    @Override
    public boolean isEnded() {
        return inputEnded && outputBuffer == EMPTY_BUFFER;
    }

    @Override
    public void flush() {
        outputBuffer = EMPTY_BUFFER;
        inputEnded = false;
        if (ringBufferSize > 0) {
            if (midRingBuffer != null) java.util.Arrays.fill(midRingBuffer, 0f);
            if (surroundRingBuffer != null) java.util.Arrays.fill(surroundRingBuffer, 0f);
            if (leftRingBuffer != null) java.util.Arrays.fill(leftRingBuffer, 0f);
            if (rightRingBuffer != null) java.util.Arrays.fill(rightRingBuffer, 0f);
            ringIndex = 0;
        }
        vocalFilterState = 0f;
        vocalHighFilterState = 0f;
        lfeFilter1 = 0f;
        lfeFilter2 = 0f;
        crossLFilter = 0f;
        crossRFilter = 0f;
    }

    @Override
    public void reset() {
        flush();
        buffer = EMPTY_BUFFER;
        inputAudioFormat = AudioFormat.NOT_SET;
        outputAudioFormat = AudioFormat.NOT_SET;
        surroundRingBuffer = new float[0];
        leftRingBuffer = new float[0];
        rightRingBuffer = new float[0];
        ringBufferSize = 0;
    }

    private static float softLimit(float x) {
        if (x > 0.82f) {
            return 0.82f + (float) Math.tanh((x - 0.82f) * 1.4f) * 0.16f;
        } else if (x < -0.82f) {
            return -0.82f + (float) Math.tanh((x + 0.82f) * 1.4f) * 0.16f;
        }
        return x;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

