package com.example.animelib.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.animelib.R;
import com.google.android.material.button.MaterialButton;

public class DialogUtils {

    public interface DialogListener {
        void onPositiveClick();
        void onNegativeClick();
    }

    public interface InputDialogListener {
        void onInputConfirmed(String input);
        void onCancelled();
    }

    public interface ListDialogListener {
        void onItemSelected(int index);
    }

    public interface NeutralListDialogListener {
        void onItemSelected(int index);
        void onNeutralClicked();
    }

    public interface SingleChoiceDialogListener {
        void onChoiceSelected(int index);
    }

    private static void setupDialogWindow(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
    }

    /**
     * Standard Custom Confirmation / Alert Dialog
     */
    public static void showCustomDialog(Context context, String title, String message, String posText, String negText, DialogListener listener) {
        showConfirmDialog(context, title, message, posText, negText, false, listener);
    }

    /**
     * Custom Confirmation / Alert Dialog with optional danger style
     */
    public static void showConfirmDialog(Context context, String title, String message, String posText, String negText, boolean isDanger, DialogListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_custom);
        setupDialogWindow(dialog);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        ImageButton btnClose = dialog.findViewById(R.id.btnDialogClose);
        MaterialButton btnPos = dialog.findViewById(R.id.btnDialogPositive);
        MaterialButton btnNeg = dialog.findViewById(R.id.btnDialogNegative);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);

        if (btnPos != null) {
            btnPos.setText(posText != null ? posText : "ОК");
            if (isDanger) {
                btnPos.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.dialog_danger_bg));
                btnPos.setTextColor(ContextCompat.getColor(context, R.color.dialog_danger_text));
            }
            btnPos.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onPositiveClick();
            });
        }

        if (btnNeg != null) {
            if (negText != null && !negText.isEmpty()) {
                btnNeg.setText(negText);
                btnNeg.setVisibility(View.VISIBLE);
                btnNeg.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (listener != null) listener.onNegativeClick();
                });
            } else {
                btnNeg.setVisibility(View.GONE);
            }
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onNegativeClick();
            });
        }

        dialog.show();
    }

    /**
     * Custom Input Dialog with styled EditText (e.g. for creating playlists)
     */
    public static void showInputDialog(Context context, String title, String message, String defaultText, String hint, String posText, String negText, InputDialogListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_custom_input);
        setupDialogWindow(dialog);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        EditText etInput = dialog.findViewById(R.id.etDialogInput);
        ImageButton btnClose = dialog.findViewById(R.id.btnDialogClose);
        MaterialButton btnPos = dialog.findViewById(R.id.btnDialogPositive);
        MaterialButton btnNeg = dialog.findViewById(R.id.btnDialogNegative);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) {
            if (message != null && !message.isEmpty()) {
                tvMessage.setText(message);
                tvMessage.setVisibility(View.VISIBLE);
            } else {
                tvMessage.setVisibility(View.GONE);
            }
        }

        if (etInput != null) {
            if (hint != null) etInput.setHint(hint);
            if (defaultText != null) {
                etInput.setText(defaultText);
                etInput.setSelection(defaultText.length());
            }
        }

        if (btnPos != null) {
            btnPos.setText(posText != null ? posText : "Сохранить");
            btnPos.setOnClickListener(v -> {
                String text = etInput != null ? etInput.getText().toString().trim() : "";
                dialog.dismiss();
                if (listener != null) listener.onInputConfirmed(text);
            });
        }

        if (btnNeg != null) {
            btnNeg.setText(negText != null ? negText : "Отмена");
            btnNeg.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onCancelled();
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onCancelled();
            });
        }

        dialog.show();
    }

    /**
     * Custom List / Choice Dialog
     */
    public static void showListDialog(Context context, String title, String[] items, ListDialogListener listener) {
        showListWithNeutralDialog(context, title, items, null, new NeutralListDialogListener() {
            @Override
            public void onItemSelected(int index) {
                if (listener != null) listener.onItemSelected(index);
            }

            @Override
            public void onNeutralClicked() {}
        });
    }

    /**
     * Custom List Choice Dialog with optional Neutral action button
     */
    public static void showListWithNeutralDialog(Context context, String title, String[] items, String neutralText, NeutralListDialogListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_custom_list);
        setupDialogWindow(dialog);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        ImageButton btnClose = dialog.findViewById(R.id.btnDialogClose);
        LinearLayout layoutChoices = dialog.findViewById(R.id.layoutChoicesContainer);
        MaterialButton btnNeutral = dialog.findViewById(R.id.btnDialogNeutral);
        MaterialButton btnNeg = dialog.findViewById(R.id.btnDialogNegative);

        if (tvTitle != null) tvTitle.setText(title);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (layoutChoices != null && items != null) {
            layoutChoices.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(context);

            for (int i = 0; i < items.length; i++) {
                final int index = i;
                View itemView = inflater.inflate(R.layout.item_dialog_choice, layoutChoices, false);
                TextView tvChoice = itemView.findViewById(R.id.tvChoiceTitle);
                ImageView ivIndicator = itemView.findViewById(R.id.ivChoiceIndicator);

                if (tvChoice != null) {
                    tvChoice.setText(items[i]);
                }
                if (ivIndicator != null) {
                    ivIndicator.setVisibility(View.GONE);
                }

                itemView.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (listener != null) listener.onItemSelected(index);
                });

                layoutChoices.addView(itemView);
            }
        }

        if (btnNeutral != null) {
            if (neutralText != null && !neutralText.isEmpty()) {
                btnNeutral.setVisibility(View.VISIBLE);
                btnNeutral.setText(neutralText);
                btnNeutral.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (listener != null) listener.onNeutralClicked();
                });
            } else {
                btnNeutral.setVisibility(View.GONE);
            }
        }

        if (btnNeg != null) {
            btnNeg.setText("Отмена");
            btnNeg.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    /**
     * Custom Single Choice / Radio Dialog (e.g. for sorting options)
     */
    public static void showSingleChoiceDialog(Context context, String title, String[] items, int selectedIndex, SingleChoiceDialogListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_custom_list);
        setupDialogWindow(dialog);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        ImageButton btnClose = dialog.findViewById(R.id.btnDialogClose);
        LinearLayout layoutChoices = dialog.findViewById(R.id.layoutChoicesContainer);
        MaterialButton btnNeutral = dialog.findViewById(R.id.btnDialogNeutral);
        MaterialButton btnNeg = dialog.findViewById(R.id.btnDialogNegative);

        if (tvTitle != null) tvTitle.setText(title);
        if (btnNeutral != null) btnNeutral.setVisibility(View.GONE);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (layoutChoices != null && items != null) {
            layoutChoices.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(context);

            for (int i = 0; i < items.length; i++) {
                final int index = i;
                View itemView = inflater.inflate(R.layout.item_dialog_choice, layoutChoices, false);
                TextView tvChoice = itemView.findViewById(R.id.tvChoiceTitle);
                ImageView ivIndicator = itemView.findViewById(R.id.ivChoiceIndicator);

                if (tvChoice != null) {
                    tvChoice.setText(items[i]);
                    if (index == selectedIndex) {
                        tvChoice.setTextColor(ContextCompat.getColor(context, R.color.accent_color));
                    }
                }

                if (ivIndicator != null) {
                    if (index == selectedIndex) {
                        ivIndicator.setVisibility(View.VISIBLE);
                        itemView.setBackgroundResource(R.drawable.bg_sort_popup_item_selected);
                    } else {
                        ivIndicator.setVisibility(View.GONE);
                        itemView.setBackgroundResource(R.drawable.bg_popup_item_ripple);
                    }
                }

                itemView.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (listener != null) listener.onChoiceSelected(index);
                });

                layoutChoices.addView(itemView);
            }
        }

        if (btnNeg != null) {
            btnNeg.setText("Отмена");
            btnNeg.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
