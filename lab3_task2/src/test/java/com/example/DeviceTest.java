package com.example;


import org.junit.Test;


import com.example.modeling.Model;
import com.example.modeling.components.CompDevice;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Producer;
import com.example.modeling.components.Queue;
import com.example.modeling.components.device.Device.DeviceRand;
import com.example.modeling.Process;

public class DeviceTest 
{
    @Test
    public void deviceTest()
    {
        var producer = new Producer(
            (_) -> Model.getExponential(1.0).get(), 
            () -> {
                double x = Model.r.nextDouble(); 
                
                if (x < 0.5) return 1;          // 0.5
                else if (x < 0.6) return 2;     // 0.1
                else return 3;                  // 0.4
            }, 
            "Producer"
        );

        //reception
        var receptionQueue = new Queue("receptionQueue");
        var receptionConnection = new Connection(new Model.Priority(), (typ) -> typ, "receptionConnection");

        DeviceRand randDoctor = (typ) -> {
            return switch(typ) {
                case 1 -> Model.getExponential(15.0).get();
                case 2 -> Model.getExponential(40.0).get();
                case 3 -> Model.getExponential(30.0).get();
                default -> throw new IllegalArgumentException("typ = " + typ);
            };
        };

        var doctor1 = new CompDevice(randDoctor, "Doctor1");
        var doctor2 = new CompDevice(randDoctor, "Doctor2");

        producer.setNext(receptionQueue);
        receptionQueue.setNext(receptionConnection);

        receptionConnection.addNext(doctor1, 1);
        receptionConnection.addNext(doctor2, 1);

        var proc = new Process(producer, "MainProc");
        proc.run(1000.0);
        System.out.println(proc.getStats());
    }
}
