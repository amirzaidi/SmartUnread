package amirz.plugin.unread;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.android.launcher3.Utilities;
import com.android.launcher3.notification.NotificationListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import amirz.plugin.unread.calendar.CalendarParser;
import amirz.plugin.unread.calendar.CalendarReceiver;
import amirz.plugin.unread.calendar.DateBroadcastReceiver;
import amirz.plugin.unread.media.MediaListener;
import amirz.plugin.unread.notifications.NotificationList;
import amirz.plugin.unread.notifications.NotificationRanker;
import amirz.plugin.unread.notifications.ParsedNotification;
import amirz.plugin.unread.widget.ConfigurationActivity;
import amirz.plugin.unread.widget.AbstractWidgetProvider;
import amirz.smartunread.R;

class UnreadSession {
    private static final int NOTIF_UPDATE_DELAY = 750;

    private final Context mContext;
    private final Handler mHandler = new Handler();

    private final List<StatusBarNotification> mSbn = new ArrayList<>();
    private final NotificationRanker mRanker = new NotificationRanker(mSbn);
    private final NotificationList mNotifications = new NotificationList(this::onNotificationsChanged);

    private final MediaListener mMedia;

    private final ClickBroadcastReceiver mPressReceiver;
    private final DateBroadcastReceiver mDateReceiver;
    private final CalendarReceiver mCalendarReceiver;
    private final BatteryBroadcastReceiver mBatteryReceiver;
    private final RotationReceiver mRotationReceiver;

    private OnClickListener mOnClick;

    // Delay updates to keep the notification showing after pressing it.
    private long mLastClick;

    public interface OnClickListener {
        void onClick();
    }

    UnreadSession(Context context) {
        mContext = context;

        mMedia = new MediaListener(context, mSbn, this::reload);

        mPressReceiver = new ClickBroadcastReceiver(context, () -> mOnClick.onClick());
        mDateReceiver = new DateBroadcastReceiver(context, this::reload);
        mCalendarReceiver = new CalendarReceiver(context, this::reload);
        mBatteryReceiver = new BatteryBroadcastReceiver(context, this::reload);
        mRotationReceiver = new RotationReceiver(context, this::reload);
    }

    void onCreate() {
        mMedia.onResume();

        mPressReceiver.onResume();
        mDateReceiver.onResume();
        mCalendarReceiver.onResume();
        mBatteryReceiver.onResume();
        mRotationReceiver.onResume();

        NotificationListener.setNotificationsChangedListener(mNotifications);
    }

    void onDestroy() {
        NotificationListener.removeNotificationsChangedListener();

        mMedia.onPause();

        mPressReceiver.onPause();
        mDateReceiver.onPause();
        mCalendarReceiver.onPause();
        mBatteryReceiver.onPause();
        mRotationReceiver.onPause();
    }

    List<String> getText() {
        List<String> textList = new ArrayList<>();

        // 1. Media
        if (mMedia.isTracking()) {
            textList.add(mMedia.getTitle().toString());
            CharSequence artist = mMedia.getArtist();
            if (TextUtils.isEmpty(artist)) {
                textList.add(getApp(mMedia.getPackage()).toString());
            } else {
                textList.add(artist.toString());
                CharSequence album = mMedia.getAlbum();
                if (!TextUtils.isEmpty(album) && !textList.contains(album.toString())) {
                    textList.add(album.toString());
                }
            }
            mOnClick = mMedia::onClick;
            return textList;
        }

        NotificationRanker.RankedNotification ranked = mRanker.getBestNotification();
        ParsedNotification parsed = null;

        // 2. High priority notification
        if (ranked != null) {
            parsed = new ParsedNotification(mContext, ranked.sbn);
            if (ranked.important) {
                addImportantNotification(textList, parsed);
                return textList;
            }
        }

        // 3. Calendar event
        UnreadSession.OnClickListener openCalendar =
                () -> startIntent(mDateReceiver.getCalendarIntent());
        mOnClick = openCalendar;
        CalendarParser.Event event = CalendarParser.getEvent(mContext);
        if (event != null) {
            textList.add(event.name);
            int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_WEEKDAY;
            textList.add(DateUtils.formatDateTime(mContext, event.start.getTimeInMillis(), flags));
            if (event.start.get(Calendar.DAY_OF_WEEK) == event.end.get(Calendar.DAY_OF_WEEK)) {
                flags &= ~DateUtils.FORMAT_SHOW_WEEKDAY;
            }
            textList.add(DateUtils.formatDateTime(mContext, event.end.getTimeInMillis(), flags));
            return textList;
        }

        // 4. Battery charging text
        if (ConfigurationActivity.chargingPerc(mContext) && mBatteryReceiver.isCharging()) {
            mOnClick = () -> startIntent(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));
            int lvl = mBatteryReceiver.getLevel();
            textList.add(lvl == 100
                    ? mContext.getString(R.string.shadespace_text_charged)
                    : mContext.getString(R.string.shadespace_text_charging,
                        mBatteryReceiver.getLevel()));
        }
        // 5. Date (Reuse open calendar onClick)
        if (ConfigurationActivity.currentDate(mContext)) {
            mOnClick = openCalendar;
            textList.add(DateUtils.formatDateTime(mContext, System.currentTimeMillis(),
                    DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE));
        }

        // 6. Low priority notification.
        if (parsed != null && ConfigurationActivity.silentNotifs(mContext)) {
            if (textList.isEmpty()) {
                addImportantNotification(textList, parsed);
            } else {
                // Remove all except top level.
                while (textList.size() > 1) {
                    textList.remove(textList.size() - 1);
                }

                for (int i = parsed.splitTitle.length - 1; i >= 0; i--) {
                    textList.add(parsed.splitTitle[i]);
                }
                if (!TextUtils.isEmpty(parsed.body)) {
                    textList.add(parsed.body);
                }
                String app = getApp(parsed.pkg).toString();
                if (!textList.contains(app)) {
                    textList.add(app);
                }
            }
        }

        String greeting = ConfigurationActivity.greeting(mContext);
        if (!TextUtils.isEmpty(greeting) && textList.size() <= 1) {
            textList.add(greeting);
        }

        return textList;
    }

    private void addImportantNotification(List<String> textList, ParsedNotification parsed) {
        // Body on top if it is not empty.
        if (!TextUtils.isEmpty(parsed.body)) {
            textList.add(parsed.body);
        }
        for (int i = parsed.splitTitle.length - 1; i >= 0; i--) {
            textList.add(parsed.splitTitle[i]);
        }

        mOnClick = () -> {
            if (parsed.pi != null) {
                mLastClick = System.currentTimeMillis();
                startPendingIntent(parsed.pi);
            }
        };
        String app = getApp(parsed.pkg).toString();
        if (!textList.contains(app)) {
            textList.add(app);
        }
    }

    private void startIntent(Intent intent) {
        try {
            mContext.startActivity(intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    getLaunchOptions());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPendingIntent(PendingIntent pi) {
        try {
            if (Utilities.ATLEAST_MARSHMALLOW) {
                pi.send(null, 0, null, null, null, null, getLaunchOptions());
            } else {
                pi.send();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bundle getLaunchOptions() {
        return ActivityOptions.makeCustomAnimation(mContext,
                R.anim.enter_app, R.anim.exit_launcher).toBundle();
    }

    private void reload() {
        long delayTime = Math.max(0, NOTIF_UPDATE_DELAY + mLastClick - System.currentTimeMillis());
        mHandler.postDelayed(() -> AbstractWidgetProvider.updateAll(mContext), delayTime);
    }

    private CharSequence getApp(String name) {
        PackageManager pm = mContext.getPackageManager();
        try {
            return pm.getApplicationLabel(
                    pm.getApplicationInfo(name, PackageManager.GET_META_DATA));
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return name;
    }

    private void onNotificationsChanged() {
        mSbn.clear();
        if (mNotifications.hasNotifications()) {
            NotificationListener listener = NotificationListener.getInstanceIfConnected();
            if (listener != null) {
                mSbn.addAll(listener.getNotificationsForKeys(mNotifications.getKeys()));
            }
        }
        mMedia.onActiveSessionsChanged(null);
        this.reload();
    }
}
