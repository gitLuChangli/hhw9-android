package com.example.handheld.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.handheld.R;
import com.example.handheld.bean.VersionBean;

import java.util.ArrayList;

public class VersionListViewAdapter extends BaseAdapter {

    private LayoutInflater layoutInflater;
    private ArrayList<VersionBean> versionBeans = new ArrayList<>();

    public VersionListViewAdapter(Context context) {
        this.layoutInflater = LayoutInflater.from(context);
    }

    public void setData(ArrayList<VersionBean> versionBeans) {
        if (versionBeans == null || versionBeans.size() == 0) {
            this.versionBeans.clear();
        } else {
            this.versionBeans = (ArrayList<VersionBean>) versionBeans.clone();
        }

        this.notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return versionBeans.size();
    }

    @Override
    public Object getItem(int position) {
        return versionBeans.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = layoutInflater.inflate(R.layout.item_version, parent, false);
            viewHolder.textViewTitle = convertView.findViewById(R.id.text_view_title);
            viewHolder.textViewValue = convertView.findViewById(R.id.text_view_value);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.textViewTitle.setText(versionBeans.get(position).getTitle());
        viewHolder.textViewValue.setText(versionBeans.get(position).getValue());
        return convertView;
    }

    class ViewHolder {
        public TextView textViewTitle, textViewValue;
    }
}
