package com.example.animelib.util;

import android.content.Context;

import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;

/**
 * Custom ExoPlayer RenderersFactory that injects 5.1 Surround Sound AudioProcessor
 * into the audio rendering pipeline.
 */
public class SurroundRenderersFactory extends DefaultRenderersFactory {

    private final AudioProcessor surroundAudioProcessor;

    public SurroundRenderersFactory(Context context, AudioProcessor surroundAudioProcessor) {
        super(context);
        this.surroundAudioProcessor = surroundAudioProcessor;
    }

    @Override
    protected AudioSink buildAudioSink(
            Context context,
            boolean enableFloatOutput,
            boolean enableAudioTrackPlaybackParams
    ) {
        if (surroundAudioProcessor != null) {
            return new DefaultAudioSink.Builder(context)
                    .setAudioProcessors(new AudioProcessor[]{ surroundAudioProcessor })
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build();
        }
        return super.buildAudioSink(context, enableFloatOutput, enableAudioTrackPlaybackParams);
    }
}
