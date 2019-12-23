package amirz.plugin.unread.calendar;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.CalendarContract;

import amirz.plugin.unread.AutoRegisterReceiver;

public class CalendarReceiver extends AutoRegisterReceiver {
    public CalendarReceiver(Context context, Runnable onReceive) {
        super(context, onReceive);
    }

    @Override
    public IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED);
        filter.addAction(CalendarContract.ACTION_EVENT_REMINDER);
        filter.addDataScheme("content");
        filter.addDataAuthority(CalendarContract.AUTHORITY, null);
        return filter;
    }
}
