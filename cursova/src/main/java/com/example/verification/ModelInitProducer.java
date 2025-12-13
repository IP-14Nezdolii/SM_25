package com.example.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.example.modeling.Connection;
import com.example.modeling.Device;
import com.example.modeling.Model;
import com.example.modeling.Producer;
import com.example.modeling.SMO;
import com.example.modeling.utils.FunRand;
import com.example.modeling.utils.Pair;
import com.example.modeling.utils.Status;

public class ModelInitProducer {
    
    public static Supplier<Pair<Model, ResultCalculator>> getModelInit(Supplier<Double>[] params) {
        return () -> {
            ArrayList<SMO> list = new ArrayList<>();

            SMO producer = new Producer(
                "prod1", new Device(params[0], "prod"), 1);

            SMO smoWithQueue = new SMO(
                "q_smo", Integer.MAX_VALUE, List.of(new Device(FunRand.getFixed(0), "Q_Device")),2);


            Device device1 = new Device(params[1], "dev1");
            SMO loader1 = new SMO("loader1", List.of(device1), 3);

            Device device2 = new Device(params[2], "dev2");
            SMO loader2 = new SMO("loader2", List.of(device2), 3);


            Device device11 = new Device(
                FunRand.getCombined(List.of(
                    params[3], 
                    params[4]
            )), "dev11");
            SMO truck1 = new SMO("truck1", List.of(device11), 4); 

            Device device12 = new Device(
                FunRand.getCombined(List.of(
                    params[3], 
                    params[4]
                )), "dev12");
            SMO truck2 = new SMO("truck2", List.of(device12), 4);

            Device device13 = new Device(
                FunRand.getCombined(List.of(
                    params[3], 
                    params[4]
            )), "dev13");
            SMO truck3 = new SMO("truck3", List.of(device13), 4);

            Device device14 = new Device(
                FunRand.getCombined(List.of(
                    params[3], 
                    params[4]
            )), "dev14");
            SMO truck4 = new SMO("truck4", List.of(device14), 4);


            Device restDev1 = 
                new Device(params[5], "rest1"); restDev1.setStatus(Status.DONE);
            SMO rest1 = new SMO("rest1", List.of(restDev1), 5);

            Device restDev2 = 
                new Device(params[5], "rest2"); restDev2.setStatus(Status.DONE);
            SMO rest2 = new SMO("rest2", List.of(restDev2), 5);


            Device restDev11 = 
                new Device(params[6], "rest11"); restDev11.setStatus(Status.DONE);
            SMO rest11 = new SMO("rest11", List.of(restDev11), 6);

            Device restDev12 = 
                new Device(params[6], "rest12"); restDev12.setStatus(Status.DONE);
            SMO rest12 = new SMO("rest12", List.of(restDev12), 6);

            Device restDev13 = 
                new Device(params[6], "rest13"); restDev13.setStatus(Status.DONE);
            SMO rest13 = new SMO("rest13", List.of(restDev13), 6);

            Device restDev14 = 
                new Device(params[6], "rest14"); restDev14.setStatus(Status.DONE);
            SMO rest14 = new SMO("rest14", List.of(restDev14), 6);


            Connection connection1 = new Connection(2);
            Connection connection2 = new Connection();
            Connection connection3 = new Connection();

            producer.setNext(connection1);
            connection1.addNext(smoWithQueue);
            smoWithQueue.setNext(connection2);

            connection2.addNext(loader1, () -> {
                int countB = 0;

                if (device11.getStatus() == Status.READY && restDev11.getStatus() == Status.DONE) 
                    countB++;
                if (device12.getStatus() == Status.READY && restDev12.getStatus() == Status.DONE) 
                    countB++;
                if (device13.getStatus() == Status.READY && restDev13.getStatus() == Status.DONE) 
                    countB++;
                if (device14.getStatus() == Status.READY && restDev14.getStatus() == Status.DONE) 
                    countB++;

                if (device2.getStatus() == Status.BUSY)
                    countB--;

                return countB > 0 && restDev1.getStatus() == Status.DONE;
            });

            connection2.addNext(loader2, () -> {
                int countB = 0;
                
                if (device11.getStatus() == Status.READY && restDev11.getStatus() == Status.DONE) 
                    countB++;
                if (device12.getStatus() == Status.READY && restDev12.getStatus() == Status.DONE) 
                    countB++;
                if (device13.getStatus() == Status.READY && restDev13.getStatus() == Status.DONE) 
                    countB++;
                if (device14.getStatus() == Status.READY && restDev14.getStatus() == Status.DONE) 
                    countB++;

                if (device1.getStatus() == Status.BUSY) 
                    countB--;

                return countB > 0 && restDev2.getStatus() == Status.DONE;
            });

            loader1.setNext(connection3);
            loader2.setNext(connection3);

            connection3.addNext(truck1, () -> {
                return restDev11.getStatus() == Status.DONE;
            });

            connection3.addNext(truck2, () -> {
                return restDev12.getStatus() == Status.DONE;
            });

            connection3.addNext(truck3, () -> {
                return restDev13.getStatus() == Status.DONE;
            });

            connection3.addNext(truck4, () -> {
                return restDev14.getStatus() == Status.DONE;
            });

            Connection restCon1 = new Connection();
            rest1.setNext(restCon1);
            restCon1.addNext(rest1, () -> device1.getStatus() == Status.DONE);

            Connection restCon2 = new Connection();
            rest2.setNext(restCon2);
            restCon2.addNext(rest2, () -> device2.getStatus() == Status.DONE);


            Connection restCon11 = new Connection();
            rest11.setNext(restCon11);
            restCon11.addNext(rest11, () -> device11.getStatus() == Status.DONE);

            Connection restCon12 = new Connection();
            rest12.setNext(restCon12);
            restCon12.addNext(rest12, () -> device12.getStatus() == Status.DONE);

            Connection restCon13 = new Connection();
            rest13.setNext(restCon13);
            restCon13.addNext(rest13, () -> device13.getStatus() == Status.DONE);

            Connection restCon14 = new Connection();
            rest14.setNext(restCon14);
            restCon14.addNext(rest14, () -> device14.getStatus() == Status.DONE);


            list.add(producer);
            list.add(smoWithQueue);

            list.add(loader1);
            list.add(loader2);

            list.add(truck1);
            list.add(truck2);
            list.add(truck3);
            list.add(truck4);

            list.add(rest1);
            list.add(rest2);

            list.add(rest11);
            list.add(rest12);
            list.add(rest13);
            list.add(rest14);

            Model model = new Model(list);


            var loader1St = loader1.getStats().getDeviceStats().get(0);
            var loader2St = loader2.getStats().getDeviceStats().get(0);

            var rest1St = rest1.getStats().getDeviceStats().get(0);
            var rest2St = rest2.getStats().getDeviceStats().get(0);

            var smoSt = smoWithQueue.getStats();
            var smoD = smoSt.getDeviceStats().get(0);

            var truck1St = truck1.getStats().getDeviceStats().get(0);
            var truck2St = truck2.getStats().getDeviceStats().get(0);
            var truck3St = truck3.getStats().getDeviceStats().get(0);
            var truck4St = truck4.getStats().getDeviceStats().get(0);

            var rest11St = rest11.getStats().getDeviceStats().get(0);
            var rest12St = rest12.getStats().getDeviceStats().get(0);
            var rest13St = rest13.getStats().getDeviceStats().get(0);
            var rest14St = rest14.getStats().getDeviceStats().get(0);
            
            ResultCalculator calc = new ResultCalculator(
                () -> producer.getStats().getServed(), 
                () -> {
                    return smoSt.getAverageQueueSize() + 
                        smoD.getDoneTime() / smoD.getTotal();
                }, 
                () -> smoWithQueue.getStats().getServed(), 
                () -> {
                    return (loader1St.getBusyTime() + rest1St.getBusyTime() + 
                        loader2St.getBusyTime() + rest2St.getBusyTime())/ 2 / loader1St.getTotal();
                }, 
                () -> {
                    var result = (truck1St.getBusyTime() + rest11St.getBusyTime() + 
                        truck2St.getBusyTime() + rest12St.getBusyTime() +
                        truck3St.getBusyTime() + rest13St.getBusyTime() +
                        truck4St.getBusyTime() + rest14St.getBusyTime() +
                        loader1St.getBusyTime() + loader2St.getBusyTime()) / 4 / rest11St.getTotal();

                    return result;
                }, 
                () -> {
                    return (double)(
                            truck1St.getServed() + 
                            truck2St.getServed() +
                            truck3St.getServed() +
                            truck4St.getServed()) / truck1St.getTotal();
                }, 
                () -> {
                    return (
                        smoSt.getTotalWaitTime() + 
                        smoD.getDoneTime() + 
                        loader1St.getBusyTime() + 
                        loader2St.getBusyTime() +
                        truck1St.getBusyTime() +
                        truck2St.getBusyTime() +
                        truck3St.getBusyTime() +
                        truck4St.getBusyTime()
                    ) / (
                        truck1St.getServed() + 
                        truck2St.getServed() +
                        truck3St.getServed() +
                        truck4St.getServed()
                    );
                }
            );

            return Pair.createPair(model, calc);
        };
    }
}
