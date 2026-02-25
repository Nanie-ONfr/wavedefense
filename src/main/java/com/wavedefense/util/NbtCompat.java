package com.wavedefense.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.lang.reflect.Method;

/**
 * NBT compatibility layer for cross-version support (1.21.4 - 1.21.11+).
 * Handles the API change where getString/getInt/etc changed from returning
 * raw values (1.21.4-1.21.8) to returning Optional-like types (1.21.9+).
 */
public final class NbtCompat {
    private static final boolean USE_NEW_API;

    static {
        boolean newApi;
        try {
            // In 1.21.9+, getString returns OptionalString (has orElse method)
            Method m = NbtCompound.class.getMethod("getString", String.class);
            newApi = !m.getReturnType().equals(String.class);
        } catch (Exception e) {
            newApi = false;
        }
        USE_NEW_API = newApi;
    }

    private NbtCompat() {}

    public static String getString(NbtCompound nbt, String key, String defaultValue) {
        if (!nbt.contains(key)) return defaultValue;
        try {
            Object result = NbtCompound.class.getMethod("getString", String.class).invoke(nbt, key);
            if (result instanceof String s) return s;
            // New API returns wrapper with orElse
            return (String) result.getClass().getMethod("orElse", Object.class).invoke(result, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int getInt(NbtCompound nbt, String key, int defaultValue) {
        if (!nbt.contains(key)) return defaultValue;
        try {
            Object result = NbtCompound.class.getMethod("getInt", String.class).invoke(nbt, key);
            if (result instanceof Integer i) return i;
            return (int) result.getClass().getMethod("orElse", int.class).invoke(result, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static float getFloat(NbtCompound nbt, String key, float defaultValue) {
        if (!nbt.contains(key)) return defaultValue;
        try {
            Object result = NbtCompound.class.getMethod("getFloat", String.class).invoke(nbt, key);
            if (result instanceof Float f) return f;
            return (float) result.getClass().getMethod("orElse", float.class).invoke(result, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double getDouble(NbtCompound nbt, String key, double defaultValue) {
        if (!nbt.contains(key)) return defaultValue;
        try {
            Object result = NbtCompound.class.getMethod("getDouble", String.class).invoke(nbt, key);
            if (result instanceof Double d) return d;
            return (double) result.getClass().getMethod("orElse", double.class).invoke(result, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static NbtCompound getCompound(NbtCompound nbt, String key) {
        if (!nbt.contains(key)) return new NbtCompound();
        NbtElement element = nbt.get(key);
        return element instanceof NbtCompound compound ? compound : new NbtCompound();
    }

    public static NbtCompound getCompound(NbtList list, int index) {
        if (index < 0 || index >= list.size()) return new NbtCompound();
        NbtElement element = list.get(index);
        return element instanceof NbtCompound compound ? compound : new NbtCompound();
    }
}
