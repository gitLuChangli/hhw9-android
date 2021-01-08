package com.example.handheld.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.handheld.MainActivity;
import com.example.handheld.R;
import com.example.handheld.lib.CommandException;
import com.example.handheld.lib.Common;
import com.example.handheld.view.MyToast;

import java.io.UnsupportedEncodingException;

public class AccessFragment extends Fragment {

    private MainActivity activity;
    private Button buttonDeselect;
    private Spinner spinnerBank, spinnerLockArea, spinnerLockAction;
    private TextView textViewSelected;
    private EditText editTextAccess, editTextStart, editTextLength, editTextValue;
    private CheckBox checkBoxHex;
    private Button buttonRead, buttonWrite, buttonClear;
    private EditText editTextLock;
    private Button buttonLock;
    private EditText editTextKill;
    private Button buttonKill;

    final String KEY_SELECTED = "k_selected";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        View view = getLayoutInflater().inflate(R.layout.fragment_access, container, false);

        spinnerBank = view.findViewById(R.id.spinner_bank);
        ArrayAdapter<String> adapterBank = new ArrayAdapter<>(activity, R.layout.spinner_item, getResources().getStringArray(R.array.array_bank));
        spinnerBank.setAdapter(adapterBank);
        spinnerBank.setSelection(1);

        spinnerLockArea = view.findViewById(R.id.spinner_lock_area);
        ArrayAdapter<String> adapterArea = new ArrayAdapter<>(activity, R.layout.spinner_item, getResources().getStringArray(R.array.array_lock_area));
        spinnerLockArea.setAdapter(adapterArea);

        spinnerLockAction = view.findViewById(R.id.spinner_lock_action);
        ArrayAdapter<String> adapterAction = new ArrayAdapter<>(activity, R.layout.spinner_item, getResources().getStringArray(R.array.array_lock_action));
        spinnerLockAction.setAdapter(adapterAction);

        textViewSelected = view.findViewById(R.id.text_view_selected);

        editTextAccess = view.findViewById(R.id.edit_text_pwd);

        editTextStart = view.findViewById(R.id.edit_text_start);
        editTextStart.setText("2");

        editTextLength = view.findViewById(R.id.edit_text_length);
        editTextLength.setText("6");

        editTextValue = view.findViewById(R.id.edit_text_value);
        checkBoxHex = view.findViewById(R.id.check_box_hex);

        buttonRead = view.findViewById(R.id.button_read);
        buttonWrite = view.findViewById(R.id.button_write);
        buttonClear = view.findViewById(R.id.button_access_clear);
        buttonRead.setOnClickListener(onClickListener);
        buttonWrite.setOnClickListener(onClickListener);
        buttonClear.setOnClickListener(onClickListener);

        editTextLock = view.findViewById(R.id.edit_text_lock_pwd);
        buttonLock = view.findViewById(R.id.button_lock);
        buttonLock.setOnClickListener(onClickListener);
        editTextKill = view.findViewById(R.id.edit_text_kill_pwd);
        buttonKill = view.findViewById(R.id.button_kill);
        buttonKill.setOnClickListener(onClickListener);

        buttonDeselect = view.findViewById(R.id.button_deselect);
        buttonDeselect.setOnClickListener(onClickListener);

        String epc = activity.sharedPreferences.getString(KEY_SELECTED, "");
        setSelected(epc);

        setEnable(false);

        return view;
    }

    private void readTag() {
        boolean result = false;
        String access = editTextAccess.getText().toString();
        String start = editTextStart.getText().toString();
        String length = editTextLength.getText().toString();
        if (access.length() == 0) {
            MyToast.show(activity, R.string.toast_access_error);
            editTextAccess.requestFocus();
            return;
        }
        if (start.length() == 0) {
            MyToast.show(activity, R.string.toast_start_error);
            editTextStart.requestFocus();
            return;
        }
        if (length.length() == 0) {
            MyToast.show(activity, R.string.toast_length_error);
            editTextLength.requestFocus();
            return;
        }
        int bank = spinnerBank.getSelectedItemPosition();
        byte[] access_password = Common.hex2Bytes(access);
        int start_ = Integer.parseInt(start);
        int length_ = Integer.parseInt(length);

        try {
            byte[] read = activity.device.readTag(bank, start_, length_, access_password);

            String value;
            if (checkBoxHex.isChecked()) {
                value = Common.bytes2String(read, 0, read.length);
            } else {
                value = new String(read, "utf-8");
            }
            editTextValue.setText(value);
            result = true;
        } catch (CommandException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (result) {
            MyToast.show(activity, R.string.toast_read_success);
            activity.beep.playOk();
        } else {
            activity.beep.playError();
        }
    }

    private void writeTag() {
        String access = editTextAccess.getText().toString();
        String start = editTextStart.getText().toString();
        String length = editTextLength.getText().toString();
        String write = editTextValue.getText().toString().trim();
        if (access == null || access.length() == 0) {
            MyToast.show(activity, R.string.toast_access_error);
            editTextAccess.requestFocus();
            return;
        }
        if (start.length() == 0) {
            MyToast.show(activity, R.string.toast_start_error);
            editTextStart.requestFocus();
            return;
        }
        if (length.length() == 0) {
            MyToast.show(activity, R.string.toast_length_error);
            editTextLength.requestFocus();
            return;
        }
        int bank = spinnerBank.getSelectedItemPosition();
        byte[] access_password = Common.hex2Bytes(access);
        int start_ = Integer.parseInt(start);
        int length_ = Integer.parseInt(length);
        while (write.length() < length_ * 2) {
            write += " ";
        }
        byte[] write_bs;
        if (checkBoxHex.isChecked()) {
            write_bs = Common.hex2Bytes(write);
        } else {
            try {
                write_bs = write.getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                MyToast.show(activity, e.getMessage());
                return;
            }
        }
        try {
            activity.device.writeTag(bank, start_, length_, access_password, write_bs);
            MyToast.show(activity, R.string.toast_write_success);
            activity.beep.playOk();
            return;
        } catch (CommandException e) {
            MyToast.show(activity, R.string.toast_write_failed);
        }
        activity.beep.playError();
    }

    private void lockTag() {
        String access = editTextLock.getText().toString();
        if (TextUtils.isEmpty(access)) {
            MyToast.show(activity, R.string.toast_access_error);
            return;
        }
        byte[] access_password = Common.hex2Bytes(access);
        int area = spinnerLockArea.getSelectedItemPosition();
        int action = spinnerLockAction.getSelectedItemPosition();
        try {
            activity.device.lockTag(access_password, area, action);
            MyToast.show(activity, R.string.toast_lock_success);
            activity.beep.playOk();
        } catch (CommandException e) {
            e.printStackTrace();
            activity.beep.playError();
            MyToast.show(activity, R.string.toast_lock_failed);
        }
    }

    private void killTag() {
        String kill = editTextKill.getText().toString();
        if (kill.length() == 0) {
            MyToast.show(activity, R.string.toast_kill_error);
            editTextKill.requestFocus();
            return;
        }
        byte[] kill_password = Common.hex2Bytes(kill);
        try {
            activity.device.killTag(kill_password);
            activity.beep.playOk();
            MyToast.show(activity, R.string.toast_kill_success);
            return;
        } catch (CommandException e) {
            e.printStackTrace();
            activity.beep.playError();
            MyToast.show(activity, R.string.toast_kill_failed);
        }
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_read:
                    readTag();
                    break;
                case R.id.button_write:
                    writeTag();
                    break;
                case R.id.button_access_clear:
                    editTextValue.setText(null);
                    break;
                case R.id.button_lock:
                    lockTag();
                    break;
                case R.id.button_kill:
                    killTag();
                    break;
                case R.id.button_deselect:
                    try {
                        activity.device.setSelectMode(1);
                        activity.beep.playOk();
                        MyToast.show(activity, R.string.toast_deselect);
                        setSelected(null);
                    } catch (CommandException e) {
                        activity.beep.playError();
                    }
                    break;
            }
        }
    };

    public void setSelected(String tag) {
        textViewSelected.setText(tag);
        buttonDeselect.setVisibility(TextUtils.isEmpty(tag) ? View.GONE : View.VISIBLE);
        activity.sharedPreferences.edit().putString(KEY_SELECTED, tag).commit();
    }

    public void setEnable(boolean enable) {
        buttonRead.setEnabled(enable);
        buttonWrite.setEnabled(enable);
        buttonLock.setEnabled(enable);
        buttonKill.setEnabled(enable);
    }
}
