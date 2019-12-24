package amirz.plugin.unread.widget;

import amirz.smartunread.R;

public class OxygenWidgetProvider extends AbstractWidgetProvider {
    private static final float MIN_SHRINK = 0.9f;

    @Override
    int getLayoutId(boolean useGoogleSans) {
        return useGoogleSans
                ? R.layout.oxygen_widget_layout_google_sans
                : R.layout.oxygen_widget_layout;
    }

    @Override
    int getDefaultTitleSize() {
        return R.dimen.smartspace_title_size_oxygen;
    }

    @Override
    int getDefaultTextSize() {
        return R.dimen.smartspace_text_size_oxygen;
    }

    @Override
    float getMinShrink() {
        return MIN_SHRINK;
    }
}
