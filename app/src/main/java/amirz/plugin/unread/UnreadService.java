package amirz.plugin.unread;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.launcher3.notification.NotificationListener;

import java.util.List;

public class UnreadService extends NotificationListener {
    private static final String TAG = "UnreadService";

    private static UnreadSession sSession;

    public static List<String> getText() {
        if (sSession == null) {
            return null;
        }
        return sSession.getText();
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate UnreadService");
        super.onCreate();
        sSession = new UnreadSession(this);
        sSession.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy UnreadService");
        super.onDestroy();
        sSession.onDestroy();
        sSession = null;
    }
}