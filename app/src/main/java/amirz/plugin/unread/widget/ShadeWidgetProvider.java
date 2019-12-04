package amirz.plugin.unread.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.util.List;

import amirz.plugin.unread.UnreadService;
import amirz.smartunread.R;

public class ShadeWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "ShadeWidgetProvider";
    public static final String ACTION_PRESS = "amirz.plugin.unread.widget.ACTION_PRESS";
    public static final String ACTION_SETTINGS = "amirz.plugin.unread.widget.ACTION_SETTINGS";

    public ShadeWidgetProvider() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_PRESS.equals(intent.getAction())) {
            Log.d(TAG, "Redestributing press");
            context.sendBroadcast(new Intent(intent.getAction()));
        } else if (ACTION_SETTINGS.equals(intent.getAction())) {
            Log.d(TAG, "Starting icon badging observer");
            IconBadgingObserver observer = new IconBadgingObserver(context, () -> { });
            observer.onClick();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent intent = new Intent(context, ShadeWidgetProvider.class);

        String top = "";
        String bottom = "";

        List<String> text = UnreadService.getText();
        if (text != null) {
            intent.setAction(ACTION_PRESS);
            if (text.size() > 0) {
                top = text.get(0);
                if (text.size() == 2) {
                    bottom = text.get(1);
                } else if (text.size() > 2) {
                    bottom = context.getString(R.string.shadespace_subtext_double, text.get(1), text.get(2));
                }
            }
        } else {
            intent.setAction(ACTION_SETTINGS);
            top = context.getString(R.string.title_missing_notification_access);
            bottom = context.getString(R.string.title_change_settings);
        }

        Resources res = context.getResources();

        float titleSize = res.getDimension(R.dimen.smartspace_title_size);
        float textSize = res.getDimension(R.dimen.smartspace_text_size);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.shade_widget_layout);

            remoteViews.setTextViewText(R.id.shadespace_text, top);
            remoteViews.setTextViewTextSize(R.id.shadespace_text,
                    TypedValue.COMPLEX_UNIT_PX,
                    titleSize);

            remoteViews.setTextViewText(R.id.shadespace_subtext, bottom);
            remoteViews.setTextViewTextSize(R.id.shadespace_subtext,
                    TypedValue.COMPLEX_UNIT_PX,
                    textSize);

            PendingIntent pi = PendingIntent.getBroadcast(context,
                    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.shadespace_text, pi);
            remoteViews.setOnClickPendingIntent(R.id.shadespace_subtext, pi);

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    public static void updateAll(Context ctx) {
        Intent intent = new Intent(ctx, ShadeWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, getAllWidgetIds(ctx));
        ctx.sendBroadcast(intent);
    }

    private static int[] getAllWidgetIds(Context ctx) {
        ComponentName cn = new ComponentName(ctx, ShadeWidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);
        return appWidgetManager.getAppWidgetIds(cn);
    }
}
