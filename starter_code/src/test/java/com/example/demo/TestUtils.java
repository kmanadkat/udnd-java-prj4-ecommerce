package com.example.demo;

import java.lang.reflect.Field;

public class TestUtils {
    public static void injectObjects(Object target, String fieldName, Object toInject){
        boolean isPrivate=false;
        try {
            Field field = target.getClass().getDeclaredField(fieldName);

            if (!field.isAccessible()){
                field.setAccessible(true);
                isPrivate=true;
            }
            field.set(target, toInject);
            if (isPrivate){
                field.setAccessible(false);
            }

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
