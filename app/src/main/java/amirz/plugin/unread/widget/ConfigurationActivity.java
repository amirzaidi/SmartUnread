package amirz.plugin.unread.widget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import amirz.smartunread.R;

public class ConfigurationActivity extends Activity {
    private static final String USE_GOOGLE_SANS = "pref_google_sans";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new ConfigurationFragment())
                .commit();
    }

    public static class ConfigurationFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {
        private Activity mContext;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContext = getActivity();

            getPreferenceManager().setSharedPreferencesName(mContext.getPackageName());
            addPreferencesFromResource(R.xml.preferences);

            findPreference(USE_GOOGLE_SANS).setOnPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            findPreference(USE_GOOGLE_SANS).setOnPreferenceChangeListener(null);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ShadeWidgetProvider.updateAll(mContext);
            return true;
        }
    }

    public static boolean useGoogleSans(Context context) {
        return getPrefs(context).getBoolean(USE_GOOGLE_SANS, true);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
    }
}