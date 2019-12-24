package amirz.plugin.unread.widget;

import amirz.smartunread.R;

public class ShadeWidgetProvider extends AbstractWidgetProvider {
    private static final float MIN_SHRINK = 0.85f;

    @Override
    int getLayoutId(boolean useGoogleSans) {
        return useGoogleSans
                ? R.layout.shade_widget_layout_google_sans
                : R.layout.shade_widget_layout;
    }

    @Override
    int getDefaultTitleSize() {
        return R.dimen.smartspace_title_size_shade;
    }

    @Override
    int getDefaultTextSize() {
        return R.dimen.smartspace_text_size_shade;
    }
    
    @Override
    int getSidePadding() {
        return R.dimen.text_horizontal_padding_shade;
    }

    @Override
    float getMinShrink() {
        return MIN_SHRINK;
    }
}
