package amirz.plugin.unread.notifications;

import android.app.Notification;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import java.util.List;

import static android.app.Notification.PRIORITY_DEFAULT;
import static android.app.Notification.PRIORITY_MIN;

public class NotificationRanker {
    private static final int PRIORITY_AT_LEAST = PRIORITY_DEFAULT;
    private final List<StatusBarNotification> mSbn;

    public NotificationRanker(List<StatusBarNotification> sbn) {
        mSbn = sbn;
    }

    public RankedNotification getBestNotification() {
        int bestPriority = PRIORITY_MIN;
        StatusBarNotification bestNotif = null;
        for (StatusBarNotification n : mSbn) {
            Notification data = n.getNotification();

            CharSequence title = data.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence text = data.extras.getCharSequence(Notification.EXTRA_TEXT);
            boolean missingTitleAndText = TextUtils.isEmpty(title) && TextUtils.isEmpty(text);

            // Do not try using an empty notification, or an ongoing notification.
            if (!missingTitleAndText && !n.isOngoing()) {
                int priority = n.getNotification().priority;
                boolean isGroupHeader = (data.flags & Notification.FLAG_GROUP_SUMMARY) != 0;
                if (priority >= bestPriority && !isGroupHeader) {
                    bestPriority = priority;
                    bestNotif = n;
                }
            }
        }
        return bestNotif == null
                ? null
                : new RankedNotification(bestNotif, bestPriority >= PRIORITY_AT_LEAST);
    }

    public static class RankedNotification {
        public final StatusBarNotification sbn;
        public final boolean important;

        private RankedNotification(StatusBarNotification sbn, boolean important) {
            this.sbn = sbn;
            this.important = important;
        }
    }
}
