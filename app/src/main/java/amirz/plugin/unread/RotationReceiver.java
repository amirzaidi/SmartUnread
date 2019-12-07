package amirz.plugin.unread;

import android.content.Context;
import android.content.IntentFilter;

public class RotationReceiver extends AutoRegisterReceiver {
    RotationReceiver(Context context, Runnable onReceive) {
        super(context, onReceive);
    }

    @Override
    IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        return filter;
    }
}
