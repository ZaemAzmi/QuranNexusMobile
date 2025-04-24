package com.example.qurannexus.core.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import java.lang.reflect.Field;

/**
 * Custom PopupMenu that supports showing icons
 */
public class IconPopupMenu extends PopupMenu {

    public IconPopupMenu(Context context, View anchor) {
        super(context, anchor);
    }

    /**
     * Force the popup menu to show icons by using reflection to access private fields
     */
    @SuppressLint("RestrictedApi")
    public void showIcons() {
        try {
            Field field = PopupMenu.class.getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuPopupHelper = field.get(this);

            if (menuPopupHelper != null && menuPopupHelper instanceof MenuPopupHelper) {
                // For Android P and higher, this might fail due to restrictions
                // But it still works on many devices
                Field forceShowIconsField = MenuPopupHelper.class.getDeclaredField("mForceShowIcon");
                forceShowIconsField.setAccessible(true);
                forceShowIconsField.setBoolean(menuPopupHelper, true);
            }
        } catch (Exception e) {
            // Even if we can't show the icons, the menu will still work
            e.printStackTrace();
        }
    }

    /**
     * Convenience method to update icons for menu items
     */
    public void updateMenuIcon(int itemId, int iconResId) {
        MenuItem item = getMenu().findItem(itemId);
        if (item != null) {
            item.setIcon(iconResId);
        }
    }
}