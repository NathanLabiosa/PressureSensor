package com.pressure_sensor;

public class MitigatorSettings {
    public enum Type { SMALL, MEDIUM, LARGE }
    private static Type current = Type.MEDIUM;      // default

    public static void setCurrent(Type t) { current = t; }
    public static Type getCurrent()       { return current; }
}

