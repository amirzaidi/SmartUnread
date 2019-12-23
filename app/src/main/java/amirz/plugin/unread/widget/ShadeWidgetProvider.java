package amirz.plugin.unread.widget;

import amirz.smartunread.R;

public class ShadeWidgetProvider extends AbstractWidgetProvider {
    @Override
    int getLayoutId(boolean useGoogleSans) {
        return useGoogleSans
                ? R.layout.shade_widget_layout_google_sans
                : R.layout.shade_widget_layout;
    }
}
