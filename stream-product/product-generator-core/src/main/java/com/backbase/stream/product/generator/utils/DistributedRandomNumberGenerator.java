package com.backbase.stream.product.generator.utils;

import java.util.HashMap;
import java.util.Map;

public class DistributedRandomNumberGenerator {

    private Map<Integer, Double> distribution;
    private double distSum;

    public DistributedRandomNumberGenerator(Map<Integer, Double> distribution) {
        this.distribution = distribution;
    }

    public DistributedRandomNumberGenerator() {
        distribution = new HashMap<>();
    }

    public void addNumber(int value, double distribution) {
        if (this.distribution.get(value) != null) {
            distSum -= this.distribution.get(value);
        }
        this.distribution.put(value, distribution);
        distSum += distribution;
    }

    public int getDistributedRandomNumber() {
        double rand = Math.random();
        double ratio = 1.0f / distSum;
        double tempDist = 0;
        for (Integer i : distribution.keySet()) {
            tempDist += distribution.get(i);
            if (rand / ratio <= tempDist) {
                return i;
            }
        }
        return 0;
    }

}
