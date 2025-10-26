package com.example;

import com.example.modeling.components.device.Device.DeviceRand;

import org.junit.Test;

import com.example.modeling.Model;
import com.example.modeling.components.CompDevice;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Constraint;
import com.example.modeling.components.Producer;
import com.example.modeling.components.Queue;
import com.example.modeling.components.Route;
import com.example.modeling.Process;

public class LabTask 
{
    @Test
    public void task2()
    {
        var producer = new Producer(
            (_) -> Model.getExponential(15.0).get(), 
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


        //conductor
        var conductorQueue = new Queue("conductorQueue"); 
        var conductorConnection = new Connection(new Model.Priority(), (typ) -> typ, "conductorConnection");

        var escort1 = new CompDevice((_) -> Model.getUniform(3, 8).get(), "Escort1");
        var escort2 = new CompDevice((_) -> Model.getUniform(3, 8).get(), "Escort2");
        var escort3 = new CompDevice((_) -> Model.getUniform(3, 8).get(), "Escort3");


        var constr1 = new Constraint((typ)-> typ != 1, "Constraint1"); 
        var route1 = new Route((_) -> Model.getUniform(2, 5).get(), "From_reception_to_registration"); 
        
        //registration
        var registrationQueue = new Queue("registrationQueue");
        var registration = new CompDevice((_) -> Model.getErlang(3, 4.5).get(), "Registration");

        var labQueue = new Queue("labQueue");
        var labConnection = new Connection(new Model.Priority(), (typ) -> typ, "labConnection");
        var lab1 = new CompDevice((_) -> Model.getErlang(2, 4).get(), "Lab1");
        var lab2 = new CompDevice((_) -> Model.getErlang(2, 4).get(), "Lab2");

        var constr2 = new Constraint((typ)-> typ == 2, "Constraint2"); 
        var route2 = new Route((_) -> Model.getUniform(2, 5).get(), "From_lab_to_reception"); 
        var patientTransformer = new Connection(new Model.Priority(), (_) -> 1, "patientTransformer");


        producer.setNext(receptionQueue);
        receptionQueue.setNext(receptionConnection);

        receptionConnection.addNext(doctor1, 1);
        receptionConnection.addNext(doctor2, 1);

        doctor1.setNext(constr1);
        doctor2.setNext(constr1);

        constr1.setNext(route1);

        constr1.setIfFailure(conductorQueue);
        conductorQueue.setNext(conductorConnection);

        conductorConnection.addNext(escort1, 1);
        conductorConnection.addNext(escort2, 1);
        conductorConnection.addNext(escort3, 1);

        route1.setNext(registrationQueue);
        registrationQueue.setNext(registration);

        var labInput = new Connection(new Model.Priority(), (typ) -> typ, "labInput");
        registration.setNext(labInput);
        labInput.addNext(labQueue, 1);
        
        
        labQueue.setNext(labConnection);
        labConnection.addNext(lab1, 1);
        labConnection.addNext(lab2, 1);

        lab1.setNext(constr2);
        lab2.setNext(constr2);

        constr2.setNext(route2);
        route2.setNext(patientTransformer);

        patientTransformer.addNext(receptionQueue, 1);


        var proc = new Process(producer, "MainProc");
        proc.run(1000.0);
        System.out.println(proc.getStats());

        var stat1 = receptionQueue.getStats();
        var stat2 = labInput.getStats();

        //1)
        double typ1_avg_time = stat1.getAverageQueueSize() * 0.5 * 0.5 * 23.5 + 15 + 5.5;
        double typ2_avg_time = stat1.getAvgWaitTime() + 40 + 3.5 + 4.5 + 4 + 3.5 + typ1_avg_time;
        double typ3_avg_time = stat1.getAvgWaitTime() + 30 + 3.5 + 4.5 + 4;
        
        double req = producer.getStats().getProcessed();

        System.out.println("Avg time: " + (
            ((req * 0.5 * typ1_avg_time) + (req * 0.1 * typ2_avg_time) + (req * 0.4 * typ3_avg_time)) / req
        ));

        //2)
        System.out.println("Lab input delay: " + (
            stat2.getThroughput()
        ));
    }
}
