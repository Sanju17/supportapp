package com.example.owner.supportapp;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.owner.supportapp.dto.NotificationData;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Owner on 7/21/2016.
 */
public class MyCursorAdapter extends BaseAdapter {
    private final Context context;
    private List<NotificationData> notificationDatas;

    public MyCursorAdapter(Context context, List<NotificationData> notificationDatas) {
        this.context = context;
        this.notificationDatas = notificationDatas;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_row, parent, false);

        TextView date = (TextView) rowView.findViewById(R.id.date);
        TextView message = (TextView) rowView.findViewById(R.id.message);
        date.setText(notificationDatas.get(position).getDate());
        message.setText(notificationDatas.get(position).getMessage());

        return rowView;
    }

    @Override
    public int getCount() {
        return notificationDatas.size();
    }

    @Override
    public Object getItem(int location) {
        return notificationDatas.get(location);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void swapItems(List<NotificationData> notificationDatas) {
        this.notificationDatas = notificationDatas;
        notifyDataSetChanged();
    }
}