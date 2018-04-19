package com.android.deepak.filescanner;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private Context mContext;
    // child data in format of header title, child title
    private List<ScanData> dataList;

    public ExpandableListAdapter(Context context,
                                 List<ScanData> dataList) {
        this.mContext = context;

        this.dataList = dataList;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this.dataList.get(groupPosition).getFileData().get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final FileData childFileData = (FileData) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_item, null);
        }

        TextView txtListChildName = convertView.findViewById(R.id.name);
        TextView txtListChildValue = convertView.findViewById(R.id.value);

        txtListChildName.setText(childFileData.getName());
        if (getGroup(groupPosition).toString().equals(mContext.getString(R.string.top_5_ext_freq))) {
            txtListChildValue.setText(String.valueOf(childFileData.getValue()));

        } else {
            txtListChildValue.setText(String.format(Locale.getDefault(), "%.2f MB", childFileData.getValue()));
        }
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.dataList.get(groupPosition).getFileData().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.dataList.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.dataList.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        ScanData data = (ScanData) getGroup(groupPosition);
        String headerTitle = data.getTitle();
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = convertView.findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}