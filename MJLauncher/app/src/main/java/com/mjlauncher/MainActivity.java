package com.mjlauncher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String PREFS = "mjlauncher";
    private static final String KEY_DOCK = "dock";
    private static final int MAX_DOCK = 5;
    private static final long CLOCK_INTERVAL_MS = 15_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<AppItem> allApps = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());

    private SharedPreferences prefs;
    private AppAdapter adapter;
    private TextView clockView;
    private TextView dateView;
    private EditText searchBox;
    private LinearLayout dockView;

    private final Runnable clockTicker = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, CLOCK_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        clockView = findViewById(R.id.clock);
        dateView = findViewById(R.id.date);
        searchBox = findViewById(R.id.search);
        dockView = findViewById(R.id.dock);
        GridView grid = findViewById(R.id.grid);

        adapter = new AppAdapter(this);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) ->
                launch(adapter.getItem(position)));
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            showAppMenu(view, adapter.getItem(position));
            return true;
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            // Pressing search on the keyboard opens the top match
            if (actionId == EditorInfo.IME_ACTION_SEARCH && adapter.getCount() > 0) {
                launch(adapter.getItem(0));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        handler.post(clockTicker);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(clockTicker);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Pressing Home brings us back here: start fresh
        searchBox.setText("");
        hideKeyboard();
    }

    @Override
    public void onBackPressed() {
        // A launcher is the home screen, so Back clears search instead of exiting
        if (searchBox.length() > 0) {
            searchBox.setText("");
        }
    }

    private void loadApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolved = pm.queryIntentActivities(mainIntent, 0);

            List<AppItem> apps = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (ResolveInfo info : resolved) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
                apps.add(new AppItem(info.loadLabel(pm).toString(), pkg, info.loadIcon(pm)));
            }
            Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));

            runOnUiThread(() -> {
                allApps.clear();
                allApps.addAll(apps);
                adapter.setApps(allApps);
                renderDock();
            });
        }).start();
    }

    private void updateClock() {
        Date now = new Date();
        clockView.setText(timeFormat.format(now));
        dateView.setText(dateFormat.format(now));
    }

    private void launch(AppItem app) {
        if (app == null) return;
        Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (intent == null) {
            Toast.makeText(this, "Can't open " + app.label, Toast.LENGTH_SHORT).show();
            return;
        }
        hideKeyboard();
        startActivity(intent);
    }

    private void showAppMenu(View anchor, AppItem app) {
        boolean pinned = getPinned().contains(app.packageName);
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, pinned ? "Remove from dock" : "Add to dock");
        menu.getMenu().add(0, 2, 1, "App info");
        menu.getMenu().add(0, 3, 2, "Uninstall");
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    togglePinned(app.packageName);
                    return true;
                case 2:
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + app.packageName)));
                    return true;
                case 3:
                    startActivity(new Intent(Intent.ACTION_DELETE,
                            Uri.parse("package:" + app.packageName)));
                    return true;
                default:
                    return false;
            }
        });
        menu.show();
    }

    private void togglePinned(String pkg) {
        List<String> pinned = getPinned();
        if (pinned.contains(pkg)) {
            pinned.remove(pkg);
        } else if (pinned.size() >= MAX_DOCK) {
            Toast.makeText(this, "Dock is full (" + MAX_DOCK + " max)", Toast.LENGTH_SHORT).show();
            return;
        } else {
            pinned.add(pkg);
        }
        prefs.edit().putString(KEY_DOCK, TextUtils.join(",", pinned)).apply();
        renderDock();
    }

    private List<String> getPinned() {
        List<String> out = new ArrayList<>();
        for (String s : prefs.getString(KEY_DOCK, "").split(",")) {
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private void renderDock() {
        dockView.removeAllViews();

        List<AppItem> pinned = new ArrayList<>();
        for (String pkg : getPinned()) {
            AppItem app = findApp(pkg);
            if (app != null) pinned.add(app);
        }

        if (pinned.isEmpty()) {
            TextView hint = new TextView(this);
            hint.setText("Long-press an app to pin it here");
            hint.setTextColor(0xB3FFFFFF);
            hint.setTextSize(13);
            hint.setGravity(Gravity.CENTER);
            dockView.addView(hint, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            return;
        }

        for (AppItem app : pinned) {
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(app.icon);
            icon.setPadding(dp(4), dp(4), dp(4), dp(4));
            icon.setOnClickListener(v -> launch(app));
            icon.setOnLongClickListener(v -> {
                togglePinned(app.packageName);
                return true;
            });
            dockView.addView(icon, new LinearLayout.LayoutParams(0, dp(60), 1f));
        }
    }

    private AppItem findApp(String pkg) {
        for (AppItem app : allApps) {
            if (app.packageName.equals(pkg)) return app;
        }
        return null;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}