package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.example.modeling.components.CompDevice;
import com.example.modeling.components.Component;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Connection.NextPriority;
import com.example.modeling.components.Constraint;
import com.example.modeling.components.Producer;
import com.example.modeling.components.Queue;
import com.example.utils.Pair;
import com.example.modeling.Model;
import com.example.modeling.Process;

/**
 * Unit test for simple App.
 */
public class LabTask2 
{
    class Task2Priority extends NextPriority {
        private final Queue q1;
        private final Queue q2;

        public Task2Priority(Queue q1, Queue q2) {
            this.q1 = q1;
            this.q2 = q2;
        }

        @Override
        public Optional<Component> getNextChosen(ArrayList<Pair<Component, Long>> next) {
            if (this.q1.getSize() == 0) {
                return Optional.of(this.q1);
            };

            if (this.q1.getSize() <= this.q2.getSize()) {
                return Optional.of(this.q1);
            }

            return Optional.of(this.q2);
        }

    }

    class Task2Queue extends Queue {
        private Task2Queue other;
        private long counter;

        public Task2Queue(String name) {
            super(name);
        }

        public void setOther(Task2Queue other) {
            this.other = other;
        }

        public long getCounter() {
            return counter;
        }

        @Override
        public void run(double time) {
            super.run(time);

            long size = this.getSize();
            long sizeOther = other.getSize();

            if ((size > sizeOther) && (size - sizeOther >= 2)) {
                this.dequeue();
                other.enqueue();

                counter++;
            }
        }
        
    }

    @Test
    public void task2()
    {
        var producer = new Producer(Model.getExponential(1/0.5), "Producer"); 
   
        var queue1 = new Task2Queue("Queue1");
        var queue2 = new Task2Queue("Queue2");

        queue1.setOther(queue2);
        queue2.setOther(queue1);

        var constr = new Constraint(
            () -> (queue1.getSize() < 3 || queue2.getSize() < 3) && (queue1.getSize() + queue2.getSize() < 6), 
            "Constraint"
        );
        var conn = new Connection(new Task2Priority(queue1, queue2), "Connection");

        //1)
        var device1 = new CompDevice(Model.getNorm(1, 0.3), "Device1");
        var device2 = new CompDevice(Model.getNorm(1, 0.3), "Device2");

        var connection = new Connection(new Model.Priority(), "Output");

        producer.setNext(constr);
        constr.setNext(conn);

        conn.addNext(queue1, 1);
        conn.addNext(queue2, 2);

        queue1.setNext(device1);
        queue2.setNext(device2);

        device1.setNext(connection);
        device2.setNext(connection);


        //1)
        device1.process();
        device2.process();

        //3)
        queue1.process();
        queue1.process();
        queue2.process();
        queue2.process();

        //2)
        var lst = List.of(constr, conn, queue1, queue2, device1, device2, connection).reversed();
        for (Component component : lst) {
            component.run(0.1);
        }
        constr.process();


        var proc = new Process(producer, "MainProc");
        proc.run(100.0);

        System.out.println(proc.getStats());

        ///
        var queue1Stats = queue1.getStats();
        var queue2Stats = queue2.getStats();
        var constrStats = constr.getStats();
        //var connStats = conn.getStats();
        var device1Stats = device1.getStats();
        var device2Stats = device2.getStats();
        var connectionStats = connection.getStats();

        //1)
        System.out.println("Cashier1 utilization: "+device1Stats.getUtilization());
        System.out.println("Cashier2 utilization: "+device2Stats.getUtilization());

        //2)
        System.out.println("Avg clients count in bank: "+ (queue1Stats.getAverageQueueSize() + 
                                            queue2Stats.getAverageQueueSize() +
                                            device1Stats.getUtilization() +
                                            device2Stats.getUtilization())
        );

        //3)
        System.out.println("Mean client departures interval: "+connectionStats.getThroughput());

        //4)
        double clientAvgTime = 
            (queue1Stats.getTotalWaitTime() + queue2Stats.getTotalWaitTime()) / 
            (queue1Stats.getServed() + queue2Stats.getServed()) + 1;

        System.out.println("Client average time: "+clientAvgTime);

        //5)
        System.out.println("Queue1 avg size: "+queue1Stats.getAverageQueueSize());
        System.out.println("Queue2 avg size: "+queue2Stats.getAverageQueueSize());

        //6)
        System.out.println("Denied: "+constrStats.getFailureProbability());

        //7)
        System.out.println("Number of exchanges: "+(queue1.getCounter() + queue2.getCounter()));
    }
}
