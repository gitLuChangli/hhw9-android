package com.example.handheld.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.example.handheld.MainActivity;
import com.example.handheld.R;

public class BarcodeFragment extends Fragment {

    private MainActivity activity;
    private ListView listViewBarcode;
    private Button buttonScan;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        activity = (MainActivity) getActivity();
        View view = inflater.inflate(R.layout.fragment_barcode, container, false);

        listViewBarcode = view.findViewById(R.id.list_view_barcode);
        listViewBarcode.setAdapter(activity.barcodeListViewAdapter);

        buttonScan = view.findViewById(R.id.button_barcode);

        buttonScan.setOnClickListener(onClickListener);

        view.findViewById(R.id.button_barcode_clear).setOnClickListener(onClickListener);
        setEnable(false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.button_barcode) {
                activity.device.scanBarcode();
                setEnable(false);
            } else if (view.getId() == R.id.button_barcode_clear) {
                activity.barcodeListViewAdapter.setData(null);
                activity.barcodeBeans.clear();
            }
        }
    };

    public void setEnable(boolean enabled) {
        buttonScan.setEnabled(enabled);
    }
}
