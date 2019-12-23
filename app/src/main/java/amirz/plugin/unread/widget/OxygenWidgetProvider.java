package amirz.plugin.unread.widget;

import amirz.smartunread.R;

public class OxygenWidgetProvider extends AbstractWidgetProvider {
    @Override
    int getLayoutId(boolean useGoogleSans) {
        return useGoogleSans
                ? R.layout.oxygen_widget_layout_google_sans
                : R.layout.oxygen_widget_layout;
    }
}
