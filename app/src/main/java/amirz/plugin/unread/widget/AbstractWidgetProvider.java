package amirz.plugin.unread.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.List;

import amirz.plugin.unread.UnreadService;
import amirz.smartunread.R;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public abstract class AbstractWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "ShadeWidgetProvider";

    public static final String ACTION_PRESS = "amirz.plugin.unread.widget.ACTION_PRESS";
    public static final String ACTION_SETTINGS = "amirz.plugin.unread.widget.ACTION_SETTINGS";

    private static final float MIN_SHRINK = 0.85f;

    private final TextPaint mMeasureTop = new TextPaint();
    private final TextPaint mMeasureBottom = new TextPaint();

    private final TextPaint mMeasureTopGSans = new TextPaint();
    private final TextPaint mMeasureBottomGSans = new TextPaint();

    private boolean mMeasurePaintInitialized;

    public AbstractWidgetProvider() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!mMeasurePaintInitialized) {
            mMeasurePaintInitialized = true;

            LayoutInflater li = LayoutInflater.from(context);

            View widgetLayout = li.inflate(getLayoutId(false), null);
            TextView topView = widgetLayout.findViewById(R.id.shadespace_text);
            TextView bottomView = widgetLayout.findViewById(R.id.shadespace_subtext);
            mMeasureTop.set(topView.getPaint());
            mMeasureBottom.set(bottomView.getPaint());

            widgetLayout = li.inflate(getLayoutId(true), null);
            topView = widgetLayout.findViewById(R.id.shadespace_text);
            bottomView = widgetLayout.findViewById(R.id.shadespace_subtext);
            mMeasureTopGSans.set(topView.getPaint());
            mMeasureBottomGSans.set(bottomView.getPaint());
        }

        super.onReceive(context, intent);

        if (ACTION_PRESS.equals(intent.getAction())) {
            context.sendBroadcast(new Intent(intent.getAction()));
        } else if (ACTION_SETTINGS.equals(intent.getAction())) {
            IconBadgingObserver observer = new IconBadgingObserver(context, () -> { });
            observer.onClick();
        }
    }

    private void reload(Context context, AppWidgetManager appWidgetManager,
                        int[] appWidgetIds, Bundle[] appWidgetOptions) {
        Intent intent = new Intent(context, AbstractWidgetProvider.class);

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

        PendingIntent pi = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        DisplayMetrics dm = res.getDisplayMetrics();
        int orientation = config.orientation;

        float titleSize = res.getDimension(R.dimen.smartspace_title_size);
        float textSize = res.getDimension(R.dimen.smartspace_text_size);
        float sidePadding = res.getDimension(R.dimen.widget_default_padding)
                + res.getDimension(R.dimen.text_horizontal_padding);

        boolean useGoogleSans = ConfigurationActivity.useGoogleSans(context);

        for (int i = 0; i < appWidgetIds.length; i++) {
            int wDp = appWidgetOptions[i].getInt(
                    orientation == Configuration.ORIENTATION_LANDSCAPE
                            ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
                            : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);

            float wPx = TypedValue.applyDimension(COMPLEX_UNIT_DIP, wDp, dm) - 2 * sidePadding;

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), getLayoutId(useGoogleSans));

            // Top Line
            setText(remoteViews, R.id.shadespace_text, wPx, top, titleSize,
                    useGoogleSans ? mMeasureTopGSans : mMeasureTop);

            // Bottom Line
            setText(remoteViews, R.id.shadespace_subtext, wPx, bottom, textSize,
                    useGoogleSans ? mMeasureBottomGSans : mMeasureBottom);

            // Redirect onClick
            remoteViews.setOnClickPendingIntent(R.id.shadespace_content, pi);
            remoteViews.setOnClickPendingIntent(R.id.shadespace_subtext, pi);

            appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
        }
    }

    private static void setText(RemoteViews remoteViews, int viewId, float widthPx, String text,
                                float defaultTextSize, TextPaint measurePaint) {
        double ratio = Math.min(1d, widthPx / measurePaint.measureText(text));
        remoteViews.setTextViewText(viewId, text);
        remoteViews.setTextViewTextSize(viewId,
                TypedValue.COMPLEX_UNIT_PX,
                (float) Math.floor(Math.max(ratio, MIN_SHRINK) * defaultTextSize));
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Bundle[] appWidgetOptions = new Bundle[appWidgetIds.length];
        for (int i = 0; i < appWidgetIds.length; i++) {
            appWidgetOptions[i] = appWidgetManager.getAppWidgetOptions(appWidgetIds[i]);
        }
        reload(context, appWidgetManager, appWidgetIds, appWidgetOptions);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        reload(context, appWidgetManager, new int[] { appWidgetId }, new Bundle[] { newOptions });
    }

    public static void updateAll(Context ctx) {
        Class<?>[] cls = { ShadeWidgetProvider.class, OxygenWidgetProvider.class };
        for (Class<?> c : cls) {
            Intent intent = new Intent(ctx, c);

            ComponentName cn = new ComponentName(ctx, c);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);
            int[] ids = appWidgetManager.getAppWidgetIds(cn);

            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            ctx.sendBroadcast(intent);
        }
    }

    abstract int getLayoutId(boolean useGoogleSans);
}
