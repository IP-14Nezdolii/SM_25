package com.example;

import com.example.modeling.Connection;
import com.example.modeling.Device;
import com.example.modeling.SMO;
import com.example.modeling.utils.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionTest {

    private Connection connection;
    private final int GROUP_SIZE = 1;

    // Допоміжний метод для швидкого створення SMO з одним девайсом
    private SMO createSimpleSMO(String name, int queueSize) {
        Supplier<Double> fastGen = () -> 1.0;
        Device device = new Device(fastGen, name + "_Dev");
        return new SMO(name, queueSize, new ArrayList<>(Collections.singletonList(device)), 0);
    }

    @BeforeEach
    void setUp() {
        connection = new Connection(GROUP_SIZE);
    }

    @Test
    @DisplayName("Constructor and setters validation")
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Connection(0), "Group size must be > 0");
        assertThrows(IllegalArgumentException.class, () -> new Connection(-5));

        assertThrows(IllegalArgumentException.class, () -> connection.addNext(null));
        assertThrows(IllegalArgumentException.class, () -> connection.addNext(null, () -> true));
    }

    @Test
    @DisplayName("getStatus() should be READY if at least one target is READY and condition is true")
    void testGetStatus() {
        SMO target = createSimpleSMO("Target", 1);
        
        // 1. Спочатку список порожній -> BUSY (немає куди йти)
        assertEquals(Status.BUSY, connection.getStatus());

        // 2. Додаємо вільний SMO -> READY
        connection.addNext(target);
        assertEquals(Status.READY, connection.getStatus());

        // 3. Забиваємо target повністю (девайс зайнятий + черга зайнята)
        target.process(); // зайняв девайс
        target.process(); // зайняв чергу
        assertEquals(Status.BUSY, target.getStatus());
        
        // Connection бачить, що target BUSY -> Connection BUSY
        assertEquals(Status.BUSY, connection.getStatus());
    }

    @Test
    @DisplayName("getStatus() should respect boolean condition")
    void testGetStatusWithCondition() {
        SMO target = createSimpleSMO("Target", 1);
        
        // Додаємо SMO, але умова FALSE
        connection.addNext(target, () -> false);
        
        // Target вільний, але умова не пускає -> BUSY
        assertEquals(Status.BUSY, connection.getStatus());
    }

    @Test
    @DisplayName("process() triggers target SMO processing")
    void testProcessTransfer() {
        SMO target = createSimpleSMO("Target", 1);
        connection.addNext(target);

        // Викликаємо process на Connection
        connection.process();

        // Target мав отримати заявку (Requests = 1)
        assertEquals(1, target.getStats().getRequests());
    }

    @Test
    @DisplayName("Group Size logic: process() only triggers target after N calls")
    void testGroupSizeBatching() {
        int batchSize = 3;
        Connection batchConnection = new Connection(batchSize);
        SMO target = createSimpleSMO("Target", 10);
        batchConnection.addNext(target);

        // 1-й виклик
        batchConnection.process();
        assertEquals(0, target.getStats().getRequests(), "Should wait for batch");

        // 2-й виклик
        batchConnection.process();
        assertEquals(0, target.getStats().getRequests(), "Should wait for batch");

        // 3-й виклик (розмір групи досягнуто)
        batchConnection.process();
        assertEquals(1, target.getStats().getRequests(), "Should trigger target now");
        
        // 4-й виклик (новий цикл)
        batchConnection.process();
        assertEquals(1, target.getStats().getRequests()); 
    }

    @Test
    @DisplayName("process() throws exception if no target is available")
    void testProcessNoTargets() {
        // Порожній Connection
        assertThrows(IllegalStateException.class, () -> connection.process());

        // Connection з зайнятим SMO
        SMO busyTarget = createSimpleSMO("BusyTarget", 0);
        busyTarget.process(); // Зайняв єдиний девайс
        connection.addNext(busyTarget);

        assertThrows(IllegalStateException.class, () -> connection.process());
    }

    @Test
    @DisplayName("Integration: SMO1 -> Connection -> SMO2")
    void testIntegrationChain() {
        // Створюємо ланцюжок: Source -> Connection -> Sink
        SMO source = createSimpleSMO("Source", 0);
        SMO sink = createSimpleSMO("Sink", 0);
        
        source.setNext(connection);
        connection.addNext(sink);

        // 1. Подаємо заявку на Source
        source.process(); 
        // Source: Busy, Sink: Idle
        assertEquals(1, source.getStats().getRequests());
        assertEquals(0, sink.getStats().getRequests());

        // 2. Виконуємо роботу на Source
        source.run(source.getLeftTime()); // Задача виконана
        // Source: Device DONE (але ще не передав далі), Sink: Idle

        // 3. Обробка подій (handleEvent має викликати next.process())
        source.handleEvents();

        // Перевірка:
        // Source має звільнитися
        assertEquals(Status.READY, source.getStatus());
        // Sink мав отримати заявку
        assertEquals(1, sink.getStats().getRequests());
        // Sink став BUSY, бо прийняв заявку на обробку
        assertEquals(Status.BUSY, sink.getStatus());
    }

    @Test
    @DisplayName("Integration: Routing based on conditions")
    void testIntegrationRouting() {
        // Source -> Connection -> (TargetA [Blocked], TargetB [Open])
        SMO source = createSimpleSMO("Source", 0);
        
        SMO targetA = createSimpleSMO("TargetA", 0);
        SMO targetB = createSimpleSMO("TargetB", 0);

        connection.addNext(targetA, () -> false); // Заблоковано умовою
        connection.addNext(targetB, () -> true);  // Відкрито
        
        source.setNext(connection);

        // Проганяємо заявку через Source
        source.process();
        source.run(source.getLeftTime());
        source.handleEvents();

        // TargetA не мав отримати нічого
        assertEquals(0, targetA.getStats().getRequests());
        // TargetB мав отримати заявку
        assertEquals(1, targetB.getStats().getRequests());
    }
}
