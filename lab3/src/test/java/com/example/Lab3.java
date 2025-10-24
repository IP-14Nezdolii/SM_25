package com.example;

import org.junit.Test;

import com.example.modeling.Producer;
import com.example.modeling.device.ModelDevice;
import com.example.utils.DeviceRand.NextPriority;

import com.example.modeling.Connection;
import com.example.modeling.Constraint;
import com.example.modeling.Process;

/**
 * Unit test for simple App.
 */
public class Lab3 
{
    @Test
    public void Task2()
    {
        var producer = new Producer(
            NextPriority.ProbabilityWithBusy, Model.getExponential(0.5), "Producer");

        var queue1 = Model.newFixedQueue(3, "Queue1", NextPriority.Priority);
        var queue2 = Model.newFixedQueue(3, "Queue2", NextPriority.Priority);

        var device1 = Model.newDevice(Model.getExponential(0.3), "Cashier1", NextPriority.Priority);
        var device2 = Model.newDevice(Model.getExponential(0.3), "Cashier2", NextPriority.Priority);


        var con1 = new Constraint<Connection>(
            new Connection(NextPriority.Probability, "PriorityCheck1"), (_)-> {
                long size1 = queue1.getWrapped().getSize();
                long size2 = queue2.getWrapped().getSize();

                return size1 <= size2;
            });
        con1.setIfFailure(queue2);

        var con2 = new Constraint<Constraint<Connection>>(con1, (_)-> {
                return queue1.getWrapped().getSize() < 3 || queue2.getWrapped().getSize() < 3;
            });
        con2.addNext(queue1, 1);


        var con3 = new Constraint<Constraint<ModelDevice>>(
            device1, (inner)-> {
                long sizeCurr = queue1.getWrapped().getSize();
                long sizeAlt = queue2.getWrapped().getSize();

                if (inner.getWorkTime().isPresent()) {
                    if (Math.abs(sizeCurr - sizeAlt) >= 2 ) {
                        return false;
                    }
                }

                return inner.getWorkTime().isEmpty();
            });

        var con4 = new Constraint<Constraint<ModelDevice>>(
            device2, (inner)-> {
                long sizeCurr = queue2.getWrapped().getSize();
                long sizeAlt = queue1.getWrapped().getSize();

                if (inner.getWorkTime().isPresent()) {
                    if (Math.abs(sizeCurr - sizeAlt) >= 2 ) {
                        return false;
                    }
                }

                return inner.getWorkTime().isEmpty();
            });

        producer.addNext(con2, 1);

        //     

        producer.addNext(queue1, 1);
        producer.addNext(queue2, 1);

        queue1.addNext(con3, 1);
        queue2.addNext(con4, 1);

        var process = new Process(producer, "Process");

        device1.process();
        device2.process();

        queue1.process();
        queue1.process();

        queue2.process();
        queue2.process();

        process.run(100.0);

        System.out.println("simpleProcessTest:\n" + process.getStats() );
    }
}
