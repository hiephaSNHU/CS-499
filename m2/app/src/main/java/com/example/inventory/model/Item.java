package com.example.inventory.model;

public class Item {
    public final long id;
    public final String name;
    public final int quantity;

    public Item(long id, String name, int quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
    }
}
