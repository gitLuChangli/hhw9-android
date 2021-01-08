package com.example.handheld.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.example.handheld.MainActivity;
import com.example.handheld.R;
import com.example.handheld.bean.EpcBean;
import com.example.handheld.lib.Common;
import com.example.handheld.lib.SelectParam;
import com.example.handheld.view.MyToast;

public class InventoryFragment extends Fragment {

    private ImageButton imageButtonClear;
    private Button buttonInventory, buttonInventoryOnce, buttonStopInventory;
    private MainActivity activity;
    private ListView listViewInventory;
    private TextView textViewUnique, textViewSpeed;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        activity = (MainActivity) getActivity();

        View view = getLayoutInflater().inflate(R.layout.fragment_inventory, container, false);

        imageButtonClear = view.findViewById(R.id.button_clear);
        buttonInventory = view.findViewById(R.id.button_inventory);
        buttonInventoryOnce = view.findViewById(R.id.button_inventory_once);
        buttonStopInventory = view.findViewById(R.id.button_inventory_stop);

        buttonInventory.setOnClickListener(onClickListener);
        buttonInventoryOnce.setOnClickListener(onClickListener);
        buttonStopInventory.setOnClickListener(onClickListener);
        imageButtonClear.setOnClickListener(onClickListener);

        listViewInventory = view.findViewById(R.id.list_view_inventory);
        listViewInventory.setAdapter(activity.listViewAdapter);

        textViewUnique = view.findViewById(R.id.text_view_unique);
        textViewSpeed = view.findViewById(R.id.text_view_speed);

        setUnique(0);
        setSpeed(0);

        setEnable(false);

        listViewInventory.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                select(activity.epcBeans.get(i));
                return true;
            }
        });

        return view;
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_inventory:
                    activity.device.inventory();
                    //activity.device.inventoryTimes(10);
                    setEnable(false);
                    buttonStopInventory.setEnabled(true);
                    activity.initBeforeInventory();
                    break;
                case R.id.button_inventory_once:
                    activity.device.inventoryOnce();
                    activity.initBeforeInventory();
                    break;
                case R.id.button_inventory_stop:
                    activity.device.stopInventory();
                    break;
                case R.id.button_clear:
                    activity.indexes.clear();
                    activity.epcBeans.clear();
                    activity.listViewAdapter.setData(null);
                    activity.clearInventoryCache();
                    break;
            }
        }
    };

    public void setUnique(int unique) {
        textViewUnique.setText(getString(R.string.text_view_unique, unique));
    }

    public void setSpeed(long speed) {
        textViewSpeed.setText(getString(R.string.text_view_speed, speed));
    }

    public void setEnable(boolean enable) {
        buttonInventory.setEnabled(enable);
        buttonInventoryOnce.setEnabled(enable);
        buttonStopInventory.setEnabled(enable);
    }

    private void select(final EpcBean epc) {
        new AlertDialog.Builder(activity).setItems(R.array.array_select, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {

                    byte[] mask = Common.hex2Bytes(epc.getEpc());

                    SelectParam selectParam = new SelectParam();
                    selectParam.setTarget(0);
                    selectParam.setAction(0);
                    selectParam.setMemoryBank(1);
                    selectParam.setPtr(4);
                    selectParam.setMaskLength(mask.length);
                    selectParam.setTruncate(false);
                    selectParam.setMask(mask);

                    try {
                        //activity.device.setSelectParam(1, 4, mask);
                        activity.device.setSelectParam(selectParam);
                        activity.device.setSelectMode(0);
                        MyToast.show(activity, R.string.toast_select);
                        activity.beep.playOk();

                        activity.setSelected(epc.getEpc());

                    } catch (Exception e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_select_failed);
                        activity.beep.playError();
                    }
                } else if (i == 1) {
                    try {
                        activity.device.setSelectMode(1);
                        MyToast.show(activity, R.string.toast_deselect);
                        activity.beep.playOk();

                        activity.setSelected(null);

                    } catch (Exception e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_deselect_failed);
                        activity.beep.playError();
                    }
                }
            }
        }).show();
    }

    public void inventory() {
        String value = buttonInventory.getText().toString();
        String text = getString(R.string.button_inventory);
        if (value.equalsIgnoreCase(text)) {
            buttonInventory.performClick();
        }
    }
}
