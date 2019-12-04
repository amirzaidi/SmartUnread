package amirz.plugin.unread;

import android.content.Context;
import android.content.IntentFilter;

import static amirz.plugin.unread.widget.ShadeWidgetProvider.ACTION_PRESS;

class ClickBroadcastReceiver extends AutoRegisterReceiver {
    ClickBroadcastReceiver(Context context, Runnable onReceive) {
        super(context, onReceive);
    }

    @Override
    IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PRESS);
        return filter;
    }
}
