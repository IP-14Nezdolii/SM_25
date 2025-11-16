package com.example.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.example.CompDeviceWithCooldown;
import com.example.modeling.components.CompDevice;
import com.example.modeling.components.device.Device;
import com.example.modeling.utils.FunRand;

public class DeviceTest 
{
    @Test
    public void deviceTest()
    {
        var device = new Device(FunRand.getUniform(1.0, 100.0), "Device");

        double time = 0;
        for (int i = 0; i < 1_000_000; i++) {
            device.process();

            time += device.getWorkTime().get().doubleValue();
            device.run(device.getWorkTime().get());
        }

        var st = device.getStats();

        assertEquals(1_000_000, st.getServed());
        assertEquals(time, st.getBusyTime(), 0.01);
        assertEquals(0, st.getTotal() - st.getBusyTime(), 0.01);
    }

    @Test
    public void compDeviceTest()
    {
        var device = new CompDevice(FunRand.getUniform(1.0, 100.0), "Device");

        double time = 0;
        for (int i = 0; i < 1_000_000; i++) {
            device.process();

            time += device.getLeftTime().get().doubleValue();
            device.run(device.getLeftTime().get());
        }

        var st = device.getStats();

        assertEquals(1_000_000, st.getServed());
        assertEquals(time, st.getBusyTime(), 0.01);
        assertEquals(0, st.getTotal() - st.getBusyTime(), 0.01);
    }

    @Test
    public void CompDeviceWithCooldownTest()
    {
        var device = new CompDeviceWithCooldown(
            FunRand.getUniform(1.0, 100.0), 
            FunRand.getFixed(5), 
            "Device"
        );

        double time = 0;
        for (int i = 0; i < 1_000_000; i++) {
            device.process();

            time += device.getLeftTime().get().doubleValue();
            device.run(device.getLeftTime().get());

            time += device.getLeftTime().get().doubleValue();
            device.run(device.getLeftTime().get());
        }

        var st = device.getStats();

        assertEquals(1_000_000, st.getServed());
        assertEquals(time, st.getBusyTime() + st.getCooldownTime(), 0.0001);

        assertEquals(0, st.getTotal() - st.getBusyTime() - st.getCooldownTime(), 0.0001);
        assertEquals(1_000_000 * 5, st.getCooldownTime(), 0.0001);
    }
}
