/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;

/**
 * Modified from https://github.com/iFamasssxD/HorseModifier.
 *
 * @author David
 */
public class HorseNMS {

    private static String version = "";

    static {
        String name = Bukkit.getServer().getClass().getPackage().getName();
        String mcVersion = name.substring(name.lastIndexOf('.') + 1);
        version = mcVersion + ".";
    }

    public static Class<?> getCraftClass(String ClassName) {
        String className = "net.minecraft.server." + version + ClassName;
        Class<?> c = null;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return c;
    }

    public static void setSpeed(Horse horse, double value) {
        try {
            Object entityHorse = getHandle(horse);
            Object genericattributeD = getField(getCraftClass("GenericAttributes"), "d").get(null);
            Method getAttributeInstance = getMethod(getCraftClass("EntityLiving"), "getAttributeInstance", new Class<?>[]{getCraftClass("IAttribute")});
            Object iattrbute = getAttributeInstance.invoke(entityHorse, genericattributeD);
            Method setValue = getCraftClass("AttributeInstance").getMethod("setValue", double.class);
            setValue.invoke(iattrbute, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double getSpeed(Horse horse) {
        try {
            Object entityHorse = getHandle(horse);
            Object genericattributeD = getField(getCraftClass("GenericAttributes"), "d").get(null);
            Method getAttributeInstance = getMethod(getCraftClass("EntityLiving"), "getAttributeInstance", new Class<?>[]{getCraftClass("IAttribute")});
            Object iattrbute = getAttributeInstance.invoke(entityHorse, genericattributeD);
            Method getValue = getMethod(getCraftClass("AttributeInstance"), "getValue");
            return (double) getValue.invoke(iattrbute);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Object getHandle(Entity entity) throws Exception {
        Object nms_entity = null;
        Method entity_getHandle = getMethod(entity.getClass(), "getHandle");
        try {
            nms_entity = entity_getHandle.invoke(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nms_entity;
    }

    public static Method getMethod(Class<?> cl, String method) throws Exception {
        return cl.getDeclaredMethod(method);
    }

    public static Method getMethod(Class<?> cl, String method, Class<?>[] args) throws Exception {
        return cl.getDeclaredMethod(method, args);
    }

    public static Field getField(Class<?> cl, String field_name) {
        try {
            Field field = cl.getDeclaredField(field_name);
            field.setAccessible(true);
            return field;
        } catch (SecurityException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }
}
