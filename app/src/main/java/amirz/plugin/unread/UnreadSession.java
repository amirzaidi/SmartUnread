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
import com.android.launcher3.notification.NotificationInfo;
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
import amirz.plugin.unread.widget.ShadeWidgetProvider;
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

        String app = null;
        String[] splitTitle = null;
        String body = null;

        // 2. High priority notification
        if (ranked != null) {
            NotificationInfo notif = new NotificationInfo(mContext, ranked.sbn);
            app = getApp(notif.packageUserKey.mPackageName).toString();
            String title = notif.title == null
                    ? ""
                    : notif.title.toString();
            splitTitle = splitTitle(title);
            body = notif.text == null
                    ? ""
                    : notif.text.toString().trim().split("\n")[0]; // First line

            if (ranked.important) {
                // Body on top if it is not empty.
                if (!TextUtils.isEmpty(body)) {
                    textList.add(body);
                }
                for (int i = splitTitle.length - 1; i >= 0; i--) {
                    textList.add(splitTitle[i]);
                }

                PendingIntent pi = notif.intent;
                mOnClick = () -> {
                    if (pi != null) {
                        mLastClick = System.currentTimeMillis();
                        startPendingIntent(pi);
                    }
                };
                if (!textList.contains(app)) {
                    textList.add(app);
                }
                return textList;
            }
        }

        // 3. Calendar event
        mOnClick = () -> startIntent(mDateReceiver.getCalendarIntent());
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

        // 4. Date (Reuse open calendar onClick)
        textList.add(DateUtils.formatDateTime(mContext, System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE));

        // 4a. With battery charging text
        if (mBatteryReceiver.isCharging()) {
            textList.add(mContext.getString(R.string.shadespace_subtext_charging,
                    mBatteryReceiver.getLevel()));
        }
        // 4b. With normal notification
        else if (ranked != null) {
            for (int i = splitTitle.length - 1; i >= 0; i--) {
                textList.add(splitTitle[i]);
            }
            if (!TextUtils.isEmpty(body)) {
                textList.add(body);
            }
            if (!textList.contains(app)) {
                textList.add(app);
            }
        }

        return textList;
    }

    private void startIntent(Intent intent) {
        try {
            mContext.startActivity(intent, getLaunchOptions());
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
        mHandler.postDelayed(() -> ShadeWidgetProvider.updateAll(mContext), delayTime);
    }

    private String[] splitTitle(String title) {
        final String[] delimiters = { ": ", " - ", " • " };
        for (String del : delimiters) {
            if (title.contains(del)) {
                return title.split(del, 2);
            }
        }
        return new String[] { title };
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
