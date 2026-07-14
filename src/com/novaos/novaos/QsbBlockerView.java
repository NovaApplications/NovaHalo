package com.novaos.novaos;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.BatteryManager;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.novaos.novaos.config.FeatureFlags;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QsbBlockerView extends FrameLayout implements Workspace.OnStateChangeListener {
    public static final Property<QsbBlockerView, Integer> QSB_BLOCKER_VIEW_ALPHA = new QsbBlockerViewAlpha(Integer.TYPE, "bgAlpha");
    private final Paint mBgPaint = new Paint(1);

    private TextView mDateText;
    private TextView mInfoText;
    private ImageView mInfoIcon;
    private TextView mMusicText;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_TIME_TICK.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action)
                    || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                updateDate();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                updateBattery(intent);
            } else if ("com.android.music.metachanged".equals(action)
                    || "com.spotify.music.metadatachanged".equals(action)) {
                updateMusic(intent);
            }
        }
    };

    public QsbBlockerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mBgPaint.setColor(-1);
        mBgPaint.setAlpha(0);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mDateText = findViewById(R.id.now_bar_date);
        mInfoText = findViewById(R.id.now_bar_info_text);
        mInfoIcon = findViewById(R.id.now_bar_info_icon);
        mMusicText = findViewById(R.id.now_bar_music_text);
        
        if (mDateText != null) {
            mDateText.setOnClickListener(v -> openCalendar());
        }
        setOnClickListener(v -> openGoogleApp());

        updateDate();
    }

    private void openCalendar() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        } catch (Exception e) {
            openGoogleApp();
        }
    }

    private void openGoogleApp() {
        try {
            Intent intent = getContext().getPackageManager().getLaunchIntentForPackage("com.google.android.googlequicksearchbox");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
        } catch (Exception e) {
            // Ignored
        }
    }

    private void updateDate() {
        if (mDateText != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
            mDateText.setText(sdf.format(new Date()));
        }
    }

    private void updateBattery(Intent intent) {
        if (mInfoText == null || mInfoIcon == null) return;

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;

        if (isCharging) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) (level * 100 / (float) scale);
            
            mInfoText.setText(getContext().getString(R.string.battery_level_template, batteryPct));
            mInfoText.setVisibility(VISIBLE);
            mInfoIcon.setVisibility(VISIBLE);
        } else {
            mInfoText.setVisibility(GONE);
            mInfoIcon.setVisibility(GONE);
        }
    }

    private void updateMusic(Intent intent) {
        if (mMusicText == null) return;

        String artist = intent.getStringExtra("artist");
        String track = intent.getStringExtra("track");

        if (track != null) {
            mMusicText.setText(artist != null ? artist + " - " + track : track);
            mMusicText.setVisibility(VISIBLE);
            // Hide date if music is playing to save space on small screens? 
            // Or just let it be. Let's keep both for now.
        } else {
            mMusicText.setVisibility(GONE);
        }
    }

    @Override
    public void setPadding(int i, int i2, int i3, int i4) {
        super.setPadding(0, 0, 0, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Workspace workspace = Launcher.getLauncher(getContext()).getWorkspace();
        workspace.setOnStateChangeListener(this);
        prepareStateChange(workspace.getState(), null);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction("com.android.music.metachanged");
        filter.addAction("com.spotify.music.metadatachanged");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getContext().registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            getContext().registerReceiver(mReceiver, filter);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mReceiver);
    }

    @Override
    public void prepareStateChange(Workspace.State state, AnimatorSet animatorSet) {
        int i;
        if (state == Workspace.State.SPRING_LOADED) {
            i = 60;
        } else {
            i = 0;
        }
        if (animatorSet == null) {
            QSB_BLOCKER_VIEW_ALPHA.set(this, i);
            return;
        }
        animatorSet.play(ObjectAnimator.ofInt(this, QSB_BLOCKER_VIEW_ALPHA, i));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPaint(mBgPaint);
    }

    private static final class QsbBlockerViewAlpha extends Property<QsbBlockerView, Integer> {

        public QsbBlockerViewAlpha(Class<Integer> type, String name) {
            super(type, name);
        }

        @Override
        public void set(QsbBlockerView qsbBlockerView, Integer num) {
            qsbBlockerView.mBgPaint.setAlpha(num);
            qsbBlockerView.setWillNotDraw(num == 0);
            qsbBlockerView.invalidate();
        }

        @Override
        public Integer get(QsbBlockerView obj) {
            return obj.mBgPaint.getAlpha();
        }

    }
}
