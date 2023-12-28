package com.libx.ui.helpers;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtils {
    private static final String TAG = "ReflectUtils";

    public static Object genericGetField(Class<?> cl, Object obj, String fieldName) {
        Field field;
        Object requiredObj = null;
        try {
            field = cl.getDeclaredField(fieldName);
            field.setAccessible(true);
            requiredObj = field.get(obj);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            Log.e(TAG, "genericGetField", e);
        }

        return requiredObj;
    }
}