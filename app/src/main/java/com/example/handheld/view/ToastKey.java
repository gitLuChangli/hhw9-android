package com.example.handheld.view;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.handheld.R;

public class ToastKey {

    private static Toast toast;

    private ToastKey() {

    }

    public static void show(Context context, String msg) {
        cancel();
        View view = LayoutInflater.from(context).inflate(R.layout.layout_key, null);
        TextView text = view.findViewById(R.id.text_view_toast);
        text.setText(msg);
        view.setAlpha(0.55f);
        toast = new Toast(context);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.show();
    }

    public static void cancel() {
        if (toast != null) {
            toast.cancel();
        }
    }
}
