package com.example;

import com.example.modeling.Connection;
import com.example.modeling.Device;
import com.example.modeling.Producer;
import com.example.modeling.SMO;
import com.example.modeling.utils.Status;
import org.decimal4j.immutable.Decimal6f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ProducerTest {

    private Producer producer;
    private ArrayList<Device> devices;
    private final String PRODUCER_NAME = "GenSource";
    
    // Генератор часу (завжди повертає 10.0)
    private final Supplier<Double> fixedTimeGenerator = () -> 10.0;

    @BeforeEach
    void setUp() {
        devices = new ArrayList<>();
        // Створюємо 2 пристрої
        devices.add(new Device(fixedTimeGenerator, "GenDev1"));
        devices.add(new Device(fixedTimeGenerator, "GenDev2"));
        
        // Producer: черга 0 (через super), пріоритет 0
        producer = new Producer(PRODUCER_NAME, devices, 0);
    }

    @Test
    @DisplayName("process() should throw UnsupportedOperationException")
    void testProcessForbidden() {
        // Producer сам генерує заявки, зовнішній виклик process() заборонений
        assertThrows(UnsupportedOperationException.class, () -> {
            producer.process();
        }, "Producer should not accept external tasks");
    }

    @Test
    @DisplayName("handleEvent() should automatically fill all free devices (Start generation)")
    void testHandleEventAutoGeneration() {
        // 1. Початковий стан - пристрої вільні
        assertEquals(Status.BUSY, producer.getStatus());

        // 2. Викликаємо handleEvent. 
        // Логіка Producer: поки є READY, викликати super.process()
        producer.handleEvents();

        // 3. Перевірка
        // Оскільки у нас 2 девайси, Producer мав згенерувати 2 заявки і зайняти обидва девайси.
        assertEquals(Status.BUSY, producer.getStatus(), "Producer should be BUSY after auto-filling devices");
        assertEquals(2, producer.getStats().getRequests(), "Should have generated 2 requests");
        
        // Перевіримо, що девайси дійсно зайняті
        // (Спробуємо "прогнати" час, щоб переконатися, що робота йде)
        producer.run(Decimal6f.valueOf(5.0));
        assertEquals(5.0, producer.getStats().getTotalTime());
    }

    @Test
    @DisplayName("handleEvent() should recycle done tasks and immediately generate new ones")
    void testCycleGeneration() {
        // Підключаємо Producer до фейкового приймача через Connection, 
        // щоб перевірити, що заявки йдуть далі.
        SMO sink = createSinkSMO(); 
        Connection connection = new Connection(1);
        connection.addNext(sink);
        producer.setNext(connection);

        // КРОК 1: Виконання роботи
        producer.run(producer.getLeftTime()); 
        // Девайси закінчили роботу (Status DONE).
        // Served: 2 (але ще не передані в sink)
        
        // КРОК 2: Обробка подій (звільнення + нова генерація)
        producer.handleEvents();
        
        // Що мало статися всередині handleEvent:
        // 1. super.handleEvent() -> передає 2 виконані заявки в Connection -> Sink
        // 2. Девайси звільняються (стають READY).
        // 3. Loop while(READY) -> Producer знову займає їх новими задачами.

        // ПЕРЕВІРКИ:
        
        // 1. Заявки пішли в Sink?
        assertEquals(2, sink.getStats().getRequests(), "Sink should receive completed tasks");
        
        // 2. Producer знову згенерував нові задачі?
        // Було 2, згенерував ще 2 = 4
        assertEquals(4, producer.getStats().getRequests(), "Should generate new tasks immediately after freeing devices");
        
        // 3. Producer знову зайнятий?
        assertEquals(Status.BUSY, producer.getStatus(), "Producer should stay BUSY keeping devices utilized");
    }

    // --- Helper ---
    private SMO createSinkSMO() {
        ArrayList<Device> sinkDevices = new ArrayList<>();
        sinkDevices.add(new Device(() -> 1.0, "SinkDev1"));
        return new SMO("Sink", 10, sinkDevices, 0);
    }
}
