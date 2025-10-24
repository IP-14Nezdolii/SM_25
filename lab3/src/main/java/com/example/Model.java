package com.example;

import java.util.Random;

import com.example.modeling.Constraint;
import com.example.modeling.Process;
import com.example.modeling.Queue;
import com.example.modeling.device.ModelDevice;
import com.example.utils.DeviceRand;
import com.example.utils.DeviceRand.NextPriority;

public final class Model {
    private static Random r = new Random();

    public static DeviceRand getFixed(double num) {
        return () -> num;
    }

    public static DeviceRand getUniform(double from, double to) {
        return () -> {
            double n = r.nextDouble();
            while (n == 0.0) {
                n = r.nextDouble();
            }

            return from + (to - from) * n;
        };
    }

    public static DeviceRand getExponential(double lambda) {
        return () -> {
            double n = r.nextDouble();
            while (n == 0.0) {
                n = r.nextDouble();
            }

            return -(1.0 / lambda) * Math.log(n);
        };
    }

    public static Constraint<ModelDevice> newDevice(DeviceRand rand, String name, NextPriority priority) {
        return new Constraint<ModelDevice>(new ModelDevice(priority, rand, name), (device) -> device.getWorkTime().isEmpty());
    }

    public static Constraint<Queue> newFixedQueue(int maxQueueSize, String name, NextPriority priority) {
        return new Constraint<Queue>(new Queue(priority, name), (Queue queue) -> queue.getSize() < maxQueueSize);
    }

    public static Process newSimpleProcess(DeviceRand rand, int maxQueueSize, String name) {
        var queue = newFixedQueue(maxQueueSize, name + "_queue", NextPriority.Priority);
        var device = newDevice(rand, name + "_device", NextPriority.Priority);  
        
        queue.addNext(device, 1);

        return new Process(queue, name);
    }
}
