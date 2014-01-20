/*
 *  Copyright (C) 2013 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.omnirom.omnigears.interfacesettings;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.WindowManagerGlobal;

public class BarsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "BarsSettings";

    // ListView Animations Key
    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";

    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    private static final String STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";
    private static final String STATUS_BAR_TRAFFIC = "status_bar_traffic";
    private static final String STATUS_BAR_NETWORK_ACTIVITY = "status_bar_network_activity";
    private static final String QUICK_PULLDOWN = "quick_pulldown";
    private static final String QUICKSETTINGS_DYNAMIC = "quicksettings_dynamic_row";
    private static final String CATEGORY_NAVBAR = "category_navigation_bar";
    private static final String SOFT_BACK_KILL_APP = "soft_back_kill_app";

    private CheckBoxPreference mStatusBarBrightnessControl;
    private CheckBoxPreference mStatusBarNotifCount;
    private CheckBoxPreference mStatusBarTraffic;
    private CheckBoxPreference mStatusBarNetworkActivity;
    private CheckBoxPreference mQuickSettingsDynamic;
    private ListPreference mQuickPulldown;
    private CheckBoxPreference mSoftBackKillApp;

    // ListView Animations Preference
    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bars_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mStatusBarBrightnessControl =
                (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        mStatusBarBrightnessControl.setChecked((Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0) == 1));
        mStatusBarBrightnessControl.setOnPreferenceChangeListener(this);

        try {
            if (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                mStatusBarBrightnessControl.setEnabled(false);
                mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
            }
        } catch (SettingNotFoundException e) {
        }

        mStatusBarNotifCount = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NOTIF_COUNT);
        mStatusBarNotifCount.setChecked(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NOTIF_COUNT, 0) == 1);
        mStatusBarNotifCount.setOnPreferenceChangeListener(this);

        mStatusBarTraffic = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_TRAFFIC);
        int intState = Settings.System.getInt(resolver, Settings.System.STATUS_BAR_TRAFFIC, 0);
        intState = setStatusBarTrafficSummary(intState);
        mStatusBarTraffic.setChecked(intState > 0);
        mStatusBarTraffic.setOnPreferenceChangeListener(this);

        mStatusBarNetworkActivity =
                (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NETWORK_ACTIVITY);
        mStatusBarNetworkActivity.setChecked(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_ACTIVITY, 0) == 1);
        mStatusBarNetworkActivity.setOnPreferenceChangeListener(this);

        mQuickPulldown = (ListPreference) findPreference(QUICK_PULLDOWN);
        mQuickPulldown.setOnPreferenceChangeListener(this);
        int statusQuickPulldown =
                Settings.System.getInt(resolver, Settings.System.QS_QUICK_PULLDOWN,0);
        mQuickPulldown.setValue(String.valueOf(statusQuickPulldown));

        mQuickSettingsDynamic = (CheckBoxPreference) prefSet.findPreference(QUICKSETTINGS_DYNAMIC);
        mQuickSettingsDynamic.setChecked(Settings.System.getInt(resolver,
            Settings.System.QUICK_SETTINGS_TILES_ROW, 1) != 0);
        mQuickSettingsDynamic.setOnPreferenceChangeListener(this);

        // ListView Animations
        mListViewAnimation = (ListPreference) findPreference(KEY_LISTVIEW_ANIMATION);
        int listviewanimation = Settings.System.getInt(getContentResolver(),
                Settings.System.LISTVIEW_ANIMATION, 1);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) findPreference(KEY_LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.System.getInt(getContentResolver(),
                Settings.System.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this);


        boolean hasNavBar = getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);
        // Also check, if users without navigation bar force enabled it.
        hasNavBar = hasNavBar || (SystemProperties.getInt("qemu.hw.mainkeys", 1) == 0);

        // Hide navigation bar category on devices without navigation bar
        if (!hasNavBar) {
            prefSet.removePreference(findPreference(CATEGORY_NAVBAR));
        } else {
            mSoftBackKillApp = (CheckBoxPreference) prefSet.findPreference(SOFT_BACK_KILL_APP);
            mSoftBackKillApp.setChecked(Settings.System.getInt(resolver,
                    Settings.System.SOFT_BACK_KILL_APP_ENABLE, 0) == 1);
            mSoftBackKillApp.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // If we didn't handle it, let preferences handle it.
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarBrightnessControl) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL,
                    value ? 1 : 0);
        } else if (preference == mStatusBarNotifCount) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.STATUS_BAR_NOTIF_COUNT, value ? 1 : 0);
        } else if (preference == mStatusBarTraffic) {

            // Increment the state and then update the label
            int intState = Settings.System.getInt(resolver, Settings.System.STATUS_BAR_TRAFFIC, 0);
            intState++;
            intState = setStatusBarTrafficSummary(intState);
            Settings.System.putInt(resolver, Settings.System.STATUS_BAR_TRAFFIC, intState);
            if (intState > 1) {return false;}
        } else if (preference == mStatusBarNetworkActivity) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.STATUS_BAR_NETWORK_ACTIVITY,
                    value ? 1 : 0);
        } else if (preference == mQuickSettingsDynamic) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver,
                Settings.System.QUICK_SETTINGS_TILES_ROW, value ? 1 : 0);
        } else if (preference == mQuickPulldown) {
            int statusQuickPulldown = Integer.valueOf((String) objValue);
           Settings.System.putInt(resolver, Settings.System.QS_QUICK_PULLDOWN,
                   statusQuickPulldown);
        } else if (preference == mSoftBackKillApp) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver,
                Settings.System.SOFT_BACK_KILL_APP_ENABLE, value ? 1 : 0);
        } else if (preference == mListViewAnimation) {
            int listviewanimation = Integer.valueOf((String) objValue);
            int index = mListViewAnimation.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver, Settings.System.LISTVIEW_ANIMATION,
                    listviewanimation);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
            return true;
        } else if (preference == mListViewInterpolator) {
            int listviewinterpolator = Integer.valueOf((String) objValue);
            int index = mListViewInterpolator.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver, Settings.System.LISTVIEW_INTERPOLATOR,
                    listviewinterpolator);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);

            return true; 
        } else {
            return false;
        }

        return true;
    }

    private int setStatusBarTrafficSummary(int intState) {
        // These states must match com.android.systemui.statusbar.policy.Traffic
        if (intState == 1) {
            mStatusBarTraffic.setSummary(R.string.show_network_speed_bits);
        } else if (intState == 2) {
            mStatusBarTraffic.setSummary(R.string.show_network_speed_bytes);
        } else {
            mStatusBarTraffic.setSummary(R.string.show_network_speed_summary);
            return 0;
        }
        return intState;
    }
}
