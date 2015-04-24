package com.example.michael.dataserverlib;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * Created by michael on 4/22/15.
 */
public class SensorData {
    public HashMap<String,Object> getFields() {
        HashMap<String,Object> map = new HashMap<String,Object>();
        Class c = this.getClass();
        for(Field f : c.getDeclaredFields()) {
            try {
                map.put(f.getName(), f.get(this));
            } catch (IllegalAccessException e) {
                System.out.println("not allowed to get field");
            }
        }
        return map;
    }
    public void setFields(HashMap<String,Object> map) {
        Class c = this.getClass();
        for(String k : map.keySet()) {
            try {
                Field f = c.getDeclaredField(k);
                try {
                    f.set(this, f.getType().cast(map.get(k)));
                } catch (IllegalAccessException e) {
                    //Tried to set something we didn't have access to
                    System.out.println("Can't set " + k);
                } catch(ClassCastException e) {
                    //Handle a couple of special cases
                    Type t = f.getType();
                    try {
                        String s = map.get(k).toString();
                        if(t.equals(boolean.class)) {
                            f.set(this, Boolean.parseBoolean(s));
                        } else if(t.equals(byte.class)) {
                            f.set(this, Byte.parseByte(s));
                        } else if(t.equals(char.class)) {
                            f.set(this, s.charAt(0));
                        } else if(t.equals(short.class)) {
                            f.set(this, Short.parseShort(s));
                        } else if (t.equals(int.class)) {
                            f.set(this, Integer.parseInt(s));
                        } else if(t.equals(long.class)) {
                            f.set(this, Long.parseLong(s));
                        } else if(t.equals(float.class)) {
                            f.set(this, Float.parseFloat(s));
                        } else if(t.equals(double.class)) {
                            f.set(this, Double.parseDouble(s));
                        }
                    } catch (IllegalAccessException e2) {
                        e.printStackTrace();
                        System.out.println("can't do it");
                    }
                }
            } catch (NoSuchFieldException e) {
                //Just drop on floor
                System.out.println("No field " + k);
            }
        }
    }
}
