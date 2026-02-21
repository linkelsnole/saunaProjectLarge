package com.example.modbus_api.modbus.legacy;

public class Pair<T1,T2> {
    public final T1 o1;
    public T2 o2;
    public Pair(T1 o1, T2 o2) {
        this.o1 = o1;
        this.o2 = o2;
        }
    public Pair(T1 o1) {
        this.o1 = o1;
        this.o2 = null;
        }
}
