/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.iosched.util;

import java.util.List;
import java.util.TimeZone;

import novoda.droidcon.ui.AboutActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.widget.TextView;

import com.google.android.apps.iosched.provider.ScheduleContract.Blocks;
import com.google.android.apps.iosched.provider.ScheduleContract.Rooms;
import com.google.android.apps.iosched.ui.phone.MapActivity;
import com.google.android.apps.iosched.ui.tablet.MapMultiPaneActivity;
import com.novoda.droidcon2011.R;

/**
 * An assortment of UI helpers.
 */
public class UIUtils {
    /**
     * Time zone to use when formatting all session times. To always use the
     * phone local time, use {@link TimeZone#getDefault()}.
     */
    public static final TimeZone CONFERENCE_TIME_ZONE = TimeZone.getTimeZone("Europe/London");

    public static final long CONFERENCE_START_MILLIS = ParserUtils.parseTime(
            "2011-10-06T09:00:00.000+01:00");
    public static final long CONFERENCE_END_MILLIS = ParserUtils.parseTime(
            "2011-10-07T17:30:00.000+01:00");

    public static final Uri CONFERENCE_URL = Uri.parse("http://www.droidcon.co.uk/");

    /** Flags used with {@link DateUtils#formatDateRange}. */
    private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;

    private static StyleSpan sBoldSpan = new StyleSpan(Typeface.BOLD);

    /**
     * Format and return the given {@link Blocks} and {@link Rooms} values using
     * {@link #CONFERENCE_TIME_ZONE}.
     */
    public static String formatSessionSubtitle(long blockStart, long blockEnd,
            String roomName, Context context) {
        TimeZone.setDefault(CONFERENCE_TIME_ZONE);

        // NOTE: There is an efficient version of formatDateRange in Eclair and
        // beyond that allows you to recycle a StringBuilder.
        final CharSequence timeString = DateUtils.formatDateRange(context,
                blockStart, blockEnd, TIME_FLAGS);

        return context.getString(R.string.session_subtitle, timeString, roomName);
    }

    /**
     * Populate the given {@link TextView} with the requested text, formatting
     * through {@link Html#fromHtml(String)} when applicable. Also sets
     * {@link TextView#setMovementMethod} so inline links are handled.
     */
    public static void setTextMaybeHtml(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setText("");
            return;
        }
        if (text.contains("<") && text.contains(">")) {
            view.setText(Html.fromHtml(text));
            view.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            view.setText(text);
        }
    }

    public static void setSessionTitleColor(long blockStart, long blockEnd, TextView title,
            TextView subtitle) {
        long currentTimeMillis = System.currentTimeMillis();
        int colorId = R.color.body_text_1;
        int subColorId = R.color.body_text_2;

        if (currentTimeMillis > blockEnd &&
                currentTimeMillis < CONFERENCE_END_MILLIS) {
            colorId = subColorId = R.color.body_text_disabled;
        }

        final Resources res = title.getResources();
        title.setTextColor(res.getColor(colorId));
        subtitle.setTextColor(res.getColor(subColorId));
    }

    /**
     * Given a snippet string with matching segments surrounded by curly
     * braces, turn those areas into bold spans, removing the curly braces.
     */
    public static Spannable buildStyledSnippet(String snippet) {
        final SpannableStringBuilder builder = new SpannableStringBuilder(snippet);

        // Walk through string, inserting bold snippet spans
        int startIndex = -1, endIndex = -1, delta = 0;
        while ((startIndex = snippet.indexOf('{', endIndex)) != -1) {
            endIndex = snippet.indexOf('}', startIndex);

            // Remove braces from both sides
            builder.delete(startIndex - delta, startIndex - delta + 1);
            builder.delete(endIndex - delta - 1, endIndex - delta);

            // Insert bold style
            builder.setSpan(sBoldSpan, startIndex - delta, endIndex - delta - 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            delta += 2;
        }

        return builder;
    }

    public static String getLastUsedTrackID(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString("last_track_id", null);
    }

    public static void setLastUsedTrackID(Context context, String trackID) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString("last_track_id", trackID).commit();
    }

    private static final int BRIGHTNESS_THRESHOLD = 130;

    /**
     * Calculate whether a color is light or dark, based on a commonly known
     * brightness formula.
     *
     * @see {@literal http://en.wikipedia.org/wiki/HSV_color_space%23Lightness}
     */
    public static boolean isColorDark(int color) {
        return ((30 * Color.red(color) +
                59 * Color.green(color) +
                11 * Color.blue(color)) / 100) <= BRIGHTNESS_THRESHOLD;
    }

    public static boolean isHoneycomb() {
        // Can use static final constants like HONEYCOMB, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isHoneycombTablet(Context context) {
        return isHoneycomb() && isTablet(context);
    }

    public static long getCurrentTime(final Context context) {
        //SharedPreferences prefs = context.getSharedPreferences("mock_data", 0);
        //prefs.edit().commit();
        //return prefs.getLong("mock_current_time", System.currentTimeMillis());
        return System.currentTimeMillis();
    }

    public static Drawable getIconForIntent(final Context context, Intent i) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
        if (infos.size() > 0) {
            return infos.get(0).loadIcon(pm);
        }
        return null;
    }

    public static Class<? extends Activity> getMapActivityClass(Context context) {
        if (UIUtils.isHoneycombTablet(context)) {
            return MapMultiPaneActivity.class;
        }

        return MapActivity.class;
    }

    public static Class<? extends Activity> getAboutActivityClass(Context context) {
/*        if (UIUtils.isHoneycombTablet(context)) {
            return MapMultiPaneActivity.class;
        }
*/
        return AboutActivity.class;
    }
}
