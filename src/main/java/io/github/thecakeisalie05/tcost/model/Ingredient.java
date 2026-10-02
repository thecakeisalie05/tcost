package io.github.thecakeisalie05.tcost.model;

public final class Ingredient {

    public final String key;
    public final double amount;

    public Ingredient(String key, double amount) {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("key");
        if (amount <= 0) throw new IllegalArgumentException("amount");
        this.key = key;
        this.amount = amount;
    }
}
