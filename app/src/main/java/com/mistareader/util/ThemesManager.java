package com.mistareader.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;

import com.mistareader.R;

import java.util.HashMap;

public class ThemesManager {
    private final static String THEME_LIGHT = "1";
    private final static String THEME_GRAY  = "2";
    private final static String THEME_BLACK = "3";

    private static final HashMap<String, Integer> THEMES = new HashMap<>();

    static {
        THEMES.put(THEME_LIGHT, R.style.AppTheme_Light);
        THEMES.put(THEME_GRAY, R.style.AppTheme_Gray);
        THEMES.put(THEME_BLACK, R.style.AppTheme_Black);
    }

    public static int iconNewItem;
    public static int iconSend;

    public static void onActivityCreateSetTheme(Activity activity) {
        String currentTheme = Settings.getCurrentTheme(activity);
        activity.setTheme(THEMES.containsKey(currentTheme) ? THEMES.get(currentTheme) : R.style.AppTheme_Gray);
    }

    public static int getAttributeReferenceResourceId(Context context, @AttrRes int attrResId) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attrResId, outValue, true);
        return outValue.resourceId;
    }

    public static int getColorByAttr(Context context, @AttrRes int attrResId) {
        return getColorByAttr(context.getTheme(), new TypedValue(), attrResId);
        //        return ContextCompat.getColor(context, getAttributeReferenceResourceId(context, attrResId));
    }

    public static int getColorByAttr(Resources.Theme theme, TypedValue typedValue, @AttrRes int attrResId) {
        theme.resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }

    public static Drawable tint(Context context, int drawableRes, int attr) {
        int tintColor = ThemesManager.getColorByAttr(context, attr);
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        DrawableCompat.setTint(drawable, tintColor);
        return drawable;
    }

    public static void tint(Context context, Drawable drawable, int attr) {
        int tintColor = ThemesManager.getColorByAttr(context, attr);
        DrawableCompat.setTint(drawable, tintColor);
    }

    public static void tintMenu(int menuResId, Context context, MenuInflater menuInflater, Menu menu) {
        menuInflater.inflate(menuResId, menu);

        int color = getColorByAttr(context, R.attr.toolbarIconsTint);

        for (int i = 0; i < menu.size(); i++) {
            Drawable drawable = menu.getItem(i).getIcon();
            if (drawable == null) {
                continue;
            }
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, color);
        }
    }

}