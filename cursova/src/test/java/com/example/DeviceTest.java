package com.example;

import com.example.modeling.Device;
import com.example.modeling.utils.Status;
import org.decimal4j.immutable.Decimal6f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    private Device device;
    private final String DEVICE_NAME = "TestDevice";
    // Фіксований генератор для передбачуваних тестів (повертає 10.0)
    private final Supplier<Double> fixedRandom = () -> 10.0;

    @BeforeEach
    void setUp() {
        device = new Device(fixedRandom, DEVICE_NAME);
    }

    @Test
    @DisplayName("Initial state should be READY with empty stats")
    void testInitialState() {
        assertEquals(Status.READY, device.getStatus());
        assertEquals(Decimal6f.MAX_VALUE, device.getLeftTime(), "Left time should be MAX when READY");
        
        Device.Stats stats = device.getStats();
        assertEquals(0, stats.getBusyTime());
        assertEquals(0, stats.getTotal());
        assertEquals(0, stats.getServed());
        assertEquals(DEVICE_NAME, stats.getName());
    }

    @Test
    @DisplayName("process() should transition from READY to BUSY when time > 0")
    void testProcessTransitionToBusy() {
        device.process(); // Random повертає 10.0

        assertEquals(Status.BUSY, device.getStatus());
        assertEquals(Decimal6f.valueOf(10.0), device.getLeftTime());
    }

    @Test
    @DisplayName("process() should transition from READY to DONE immediately if time is 0")
    void testProcessTransitionToDone() {
        // Перевизначаємо device з генератором, що повертає 0
        Device zeroTimeDevice = new Device(() -> 0.0, "ZeroDevice");
        
        zeroTimeDevice.process();

        assertEquals(Status.DONE, zeroTimeDevice.getStatus());
        assertEquals(1, zeroTimeDevice.getStats().getServed());
    }

    @Test
    @DisplayName("process() should throw exception if not READY")
    void testProcessThrowsIfNotReady() {
        device.process(); // Стає BUSY
        
        assertThrows(IllegalStateException.class, () -> device.process(), "Should throw if called when BUSY");
        
        // Доводимо до DONE
        device.run(Decimal6f.valueOf(10.0)); 
        assertThrows(IllegalStateException.class, () -> device.process(), "Should throw if called when DONE");
    }

    @Test
    @DisplayName("run() should decrease leftTime and update stats")
    void testRunPartial() {
        device.process(); // Left time = 10.0

        Decimal6f step = Decimal6f.valueOf(4.0);
        boolean isFinished = device.run(step);

        assertFalse(isFinished);
        assertEquals(Status.BUSY, device.getStatus());
        assertEquals(Decimal6f.valueOf(6.0), device.getLeftTime()); // 10 - 4 = 6
        assertEquals(4.0, device.getStats().getBusyTime());
        assertEquals(4.0, device.getStats().getTotal());
    }

    @Test
    @DisplayName("run() should finish task when time equals leftTime")
    void testRunComplete() {
        device.process(); // Left time = 10.0

        Decimal6f step = Decimal6f.valueOf(10.0);
        boolean isFinished = device.run(step);

        assertTrue(isFinished);
        assertEquals(Status.DONE, device.getStatus());
        assertEquals(Decimal6f.MAX_VALUE, device.getLeftTime()); // Повертається до MAX при DONE
        assertEquals(1, device.getStats().getServed());
        assertEquals(10.0, device.getStats().getBusyTime());
    }

    @Test
    @DisplayName("run() should throw exception if step is greater than leftTime")
    void testRunOverflow() {
        device.process(); // Left time = 10.0

        Decimal6f tooMuch = Decimal6f.valueOf(15.0);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            device.run(tooMuch);
        });
        
        assertTrue(exception.getMessage().contains("Time step is greater than left time"));
    }

    @Test
    @DisplayName("run() should throw exception if device is not BUSY")
    void testRunWhenNotBusy() {
        // Стан READY
        assertThrows(IllegalStateException.class, () -> device.run(Decimal6f.ONE));
    }

    @Test
    @DisplayName("wait() should update total time but not busy time")
    void testWait() {
        // Стан READY
        double waitTime = 5.0;
        device.wait(Decimal6f.valueOf(waitTime));

        assertEquals(0, device.getStats().getBusyTime());
        assertEquals(waitTime, device.getStats().getTotal());
    }

    @Test
    @DisplayName("wait() should throw exception if device is BUSY")
    void testWaitWhenBusy() {
        device.process(); // Стає BUSY

        assertThrows(IllegalStateException.class, () -> device.wait(Decimal6f.ONE));
    }

    @Test
    @DisplayName("Stats should accumulate correctly over multiple steps")
    void testStatsAccumulation() {
        // 1. Чекаємо 2 сек (READY)
        device.wait(Decimal6f.valueOf(2.0));
        
        // 2. Починаємо роботу (10 сек)
        device.process();
        
        // 3. Працюємо 4 сек
        device.run(Decimal6f.valueOf(4.0));
        
        // 4. Працюємо ще 6 сек (завершуємо)
        device.run(Decimal6f.valueOf(6.0));

        Device.Stats stats = device.getStats();

        // Busy: 4 + 6 = 10
        assertEquals(10.0, stats.getBusyTime(), 0.0001);
        
        // Total: 2 (wait) + 10 (busy) = 12
        assertEquals(12.0, stats.getTotal(), 0.0001);
        
        // Served: 1
        assertEquals(1, stats.getServed());
    }
    
    @Test
    @DisplayName("setStatus() allows manual state change")
    void testSetStatus() {
        device.setStatus(Status.DONE);
        assertEquals(Status.DONE, device.getStatus());
    }

    @Test
    @DisplayName("Stats toString format check")
    void testStatsToString() {
        device.process();
        device.run(Decimal6f.valueOf(10.0)); // Served = 1, Busy = 10

        String statsString = device.getStats().toString();
        // Очікуємо: TestDevice:{busy_time=10.00, total_time=10.00, served=1}
        
        assertTrue(statsString.contains("TestDevice"));
        assertTrue(statsString.contains("busy_time=10,00") || statsString.contains("busy_time=10.00")); // Враховуємо локаль
        assertTrue(statsString.contains("served=1"));
    }
}
