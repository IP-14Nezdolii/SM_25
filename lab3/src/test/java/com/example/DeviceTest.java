package com.example;


import org.junit.Test;

import com.example.modeling.device.Device;

/**
 * Unit test for simple App.
 */
public class DeviceTest 
{
    @Test
    public void deviceTest()
    {
        var device = new Device(Model.getUniform(1.0, 100.0), "Device");

        for (int i = 0; i < 1_000_000; i++) {
            device.process();
            device.run(device.getWorkTime().get());
        }

        System.out.println(device.getStats());
    }
}
