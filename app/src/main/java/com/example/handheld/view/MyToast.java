package com.example.handheld.view;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.handheld.R;

public class MyToast {

    private static Toast toast;

    private MyToast() {

    }

    public static void show(Context context, int msgId) {
        String msg = context.getString(msgId);
        show(context, msg);
    }

    public static void show(Context context, String msg) {
        cancel();
        View view = LayoutInflater.from(context).inflate(R.layout.layout_toast, null);
        TextView text = view.findViewById(R.id.text_view_toast);
        text.setText(msg);
        toast = new Toast(context);
        toast.setGravity(Gravity.LEFT|Gravity.TOP, 16, 16);
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
