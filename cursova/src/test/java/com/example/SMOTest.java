package com.example;

import com.example.modeling.Device;
import com.example.modeling.SMO;
import com.example.modeling.utils.Status;
import org.decimal4j.immutable.Decimal6f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class SMOTest {

    private SMO smo;
    private ArrayList<Device> devices;
    private final String SMO_NAME = "TestSMO";
    
    // Генератор, який завжди повертає 10.0 для передбачуваності
    private final Supplier<Double> fixedTimeGenerator = () -> 10.0;

    @BeforeEach
    void setUp() {
        devices = new ArrayList<>();
        devices.add(new Device(fixedTimeGenerator, "Dev1"));
        devices.add(new Device(fixedTimeGenerator, "Dev2"));
        
        // Створюємо SMO з чергою розміром 1
        smo = new SMO(SMO_NAME, 1, devices, 0);
    }

    @Test
    @DisplayName("Constructor should throw exception for invalid arguments")
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> 
            new SMO("BadSMO", -1, devices, 0), "Should throw on negative queue");
            
        assertThrows(IllegalArgumentException.class, () -> 
            new SMO("BadSMO", 1, null, 0), "Should throw on null devices");
            
        assertThrows(IllegalArgumentException.class, () -> 
            new SMO("BadSMO", 1, new ArrayList<>(), 0), "Should throw on empty devices");
    }

    @Test
    @DisplayName("Initial status should be READY")
    void testInitialStatus() {
        assertEquals(Status.READY, smo.getStatus());
        assertEquals(0, smo.getStats().getRequests());
        assertEquals(0, smo.getStats().getAverageQueueSize());
    }

    @Test
    @DisplayName("process() should assign task to ready device immediately")
    void testProcessWithReadyDevice() {
        smo.process(); // Dev1 бере задачу

        // Статистика запитів має зрости
        assertEquals(1, smo.getStats().getRequests());
        
        // Один девайс зайнятий, один вільний -> СМО все ще READY
        assertEquals(Status.READY, smo.getStatus());
    }

    @Test
    @DisplayName("process() should fill queue when devices are busy")
    void testProcessFillingQueue() {
        // 1. Займаємо Dev1
        smo.process(); 
        // 2. Займаємо Dev2
        smo.process(); 
        
        // Всі девайси зайняті, але черга (розмір 1) порожня -> СМО READY
        assertEquals(Status.READY, smo.getStatus());

        // 3. Кладемо в чергу
        smo.process();
        
        // Тепер і девайси зайняті, і черга повна -> СМО BUSY
        assertEquals(Status.BUSY, smo.getStatus());
    }

    @Test
    @DisplayName("process() should throw exception when full")
    void testProcessOverflow() {
        smo.process(); // Dev1
        smo.process(); // Dev2
        smo.process(); // Queue (1/1)
        
        assertThrows(IllegalStateException.class, () -> smo.process(), "Should throw if SMO is BUSY (full)");
    }

    @Test
    @DisplayName("run() should advance time and finish tasks")
    void testRunSimulation() {
        smo.process(); // Dev1 start (task = 10.0)
        
        // Проганяємо 10 одиниць часу
        smo.run(Decimal6f.valueOf(10.0));
        
        // Dev1 мав закінчити роботу і перейти в doneDevices (всередині SMO),
        // Статистика Served має оновитися
        assertEquals(1, smo.getStats().getServed());
        assertEquals(10.0, smo.getStats().getTotalTime());
    }

    @Test
    @DisplayName("handleEvent() should recycle DONE devices to READY if no 'next' connection")
    void testHandleEventWithoutConnection() {
        smo.process(); // Dev1 зайнятий
        smo.run(Decimal6f.valueOf(10.0)); // Dev1 виконав задачу (Status DONE)
        
        // На цей момент Dev1 у списку doneDevices
        // Викликаємо handleEvent. Оскільки next == empty, він має повернути Dev1 у readyDevices
        smo.handleEvents();
        
        // Перевіряємо, що ми можемо знову дати задачу (значить є READY девайс)
        assertDoesNotThrow(() -> smo.process());
    }

    @Test
    @DisplayName("Integration: Queue processing loop")
    void testQueueProcessingLoop() {
        // Сценарій: 2 девайси, 1 місце в черзі.
        // Даємо 3 задачі.
        smo.process(); // Dev1 busy
        smo.process(); // Dev2 busy
        smo.process(); // Queue = 1
        
        // Пройшов час виконання (10.0)
        smo.run(Decimal6f.valueOf(10.0));
        
        // Dev1 і Dev2 стали DONE.
        // Викликаємо handleEvent.
        // Логіка має бути такою:
        // 1. Dev1, Dev2 повертаються в READY.
        // 2. Оскільки в черзі є заявка (Queue=1), один з них має її взяти автоматично.
        
        smo.handleEvents();
        
        // Перевірка:
        // Всього оброблено заявок (поки що ті 2, що завершились)
        assertEquals(2, smo.getStats().getServed());
        
        // Черга має стати 0, бо заявку забрав звільнений девайс
        // Примітка: у Stats queueSize не зберігається як поточне число, а як середнє.
        // Тому перевіряємо непрямим методом: спробуємо забити систему знову.
        
        // Зараз: 1 девайс працює (взяв з черги), 1 девайс вільний. Черга 0/1.
        // Маємо змогу додати ще: 1 (на вільний) + 1 (в чергу) = 2 задачі
        
        assertDoesNotThrow(() -> smo.process());
        assertDoesNotThrow(() -> smo.process());
        assertThrows(IllegalStateException.class, () -> smo.process()); // Тепер повна
    }
    
    @Test
    @DisplayName("Wait time statistics calculation")
    void testWaitTimeStats() {
        // Забиваємо девайси
        smo.process();
        smo.process();
        // Ставимо в чергу
        smo.process();
        
        // Проганяємо 5 секунд.
        // 2 девайси працюють. Заявка в черзі чекає 5 сек.
        smo.run(Decimal6f.valueOf(5.0));
        
        // Заявка в черзі почекала 5 * 1 = 5 одиниць часу totalWaitTime
        assertEquals(5.0, smo.getStats().getTotalWaitTime(), 0.001);
    }
}