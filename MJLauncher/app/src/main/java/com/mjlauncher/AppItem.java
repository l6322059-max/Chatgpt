package com.mjlauncher;

import android.graphics.drawable.Drawable;

public class AppItem {
    public final String label;
    public final String packageName;
    public final Drawable icon;

    public AppItem(String label, String packageName, Drawable icon) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
    }
}
