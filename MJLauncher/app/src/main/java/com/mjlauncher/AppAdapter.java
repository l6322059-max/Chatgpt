package com.mjlauncher;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<AppItem> all = new ArrayList<>();
    private final List<AppItem> shown = new ArrayList<>();
    private String query = "";

    public AppAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    public void setApps(List<AppItem> apps) {
        all.clear();
        all.addAll(apps);
        applyFilter();
    }

    public void setQuery(String text) {
        query = text.trim().toLowerCase(Locale.getDefault());
        applyFilter();
    }

    private void applyFilter() {
        shown.clear();
        for (AppItem app : all) {
            if (app.label.toLowerCase(Locale.getDefault()).contains(query)) {
                shown.add(app);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return shown.size();
    }

    @Override
    public AppItem getItem(int position) {
        return shown.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_app, parent, false);
        }
        AppItem app = shown.get(position);
        ((ImageView) convertView.findViewById(R.id.icon)).setImageDrawable(app.icon);
        ((TextView) convertView.findViewById(R.id.label)).setText(app.label);
        return convertView;
    }
}
