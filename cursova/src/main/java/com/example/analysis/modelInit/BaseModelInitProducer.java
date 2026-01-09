package com.example.analysis.modelInit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.example.modeling.Connection;
import com.example.modeling.Model;
import com.example.modeling.Producer;
import com.example.modeling.SingleChannelSMO;
import com.example.modeling.utils.FunRand;
import com.example.modeling.utils.Pair;

public class BaseModelInitProducer {

    public static Supplier<Pair<Model, BaseResultCalculator>> getModelInit(Supplier<Double>[] vars) {
        return () -> {
            ArrayList<SingleChannelSMO> list = new ArrayList<>();

            SingleChannelSMO producer = new Producer(
                    "prod1", vars[0], 1);

            SingleChannelSMO smoWithQueue = new SingleChannelSMO(
                    "q_smo", Integer.MAX_VALUE, FunRand.getFixed(0), 2);

            SingleChannelSMO loader1 = new SingleChannelSMO(
                    "loader1", vars[1], 3);
            SingleChannelSMO loader2 = new SingleChannelSMO(
                    "loader2", vars[2], 3);

            SingleChannelSMO truck1 = new SingleChannelSMO(
                    "truck1", FunRand.getCombined(List.of(
                            vars[4],
                            vars[5])),
                    4);

            SingleChannelSMO truck2 = new SingleChannelSMO(
                    "truck2", FunRand.getCombined(List.of(
                            vars[4],
                            vars[5])),
                    4);

            SingleChannelSMO truck3 = new SingleChannelSMO(
                    "truck3", FunRand.getCombined(List.of(
                            vars[4],
                            vars[5])),
                    4);

            SingleChannelSMO truck4 = new SingleChannelSMO(
                    "truck4", FunRand.getCombined(List.of(
                            vars[4],
                            vars[5])),
                    4);

            SingleChannelSMO rest1 = new SingleChannelSMO(
                    "rest1", vars[3], 5);
            rest1.setDoneStatus();

            SingleChannelSMO rest2 = new SingleChannelSMO(
                    "rest2", vars[3], 5);
            rest2.setDoneStatus();

            SingleChannelSMO rest11 = new SingleChannelSMO(
                    "rest11", vars[6], 6);
            rest11.setDoneStatus();

            SingleChannelSMO rest12 = new SingleChannelSMO(
                    "rest12", vars[6], 6);
            rest12.setDoneStatus();

            SingleChannelSMO rest13 = new SingleChannelSMO(
                    "rest13", vars[6], 6);
            rest13.setDoneStatus();

            SingleChannelSMO rest14 = new SingleChannelSMO(
                    "rest14", vars[6], 6);
            rest14.setDoneStatus();

            Connection connection1 = new Connection(2);
            Connection connection2 = new Connection();
            Connection connection3 = new Connection();
            Connection connection4 = new Connection();

            producer.setNext(connection1);
            connection1.addNext(smoWithQueue);
            smoWithQueue.setNext(connection2);

            connection2.addNext(loader1, () -> {
                int countB = 0;

                if (truck1.getChannelStatus().isReady() && rest11.getChannelStatus().isDone())
                    countB++;
                if (truck2.getChannelStatus().isReady() && rest12.getChannelStatus().isDone())
                    countB++;
                if (truck3.getChannelStatus().isReady() && rest13.getChannelStatus().isDone())
                    countB++;
                if (truck4.getChannelStatus().isReady() && rest14.getChannelStatus().isDone())
                    countB++;

                if (loader2.getChannelStatus().isBusy())
                    countB--;

                return countB > 0 && rest1.getChannelStatus().isDone();
            });

            connection2.addNext(loader2, () -> {
                int countB = 0;

                if (truck1.getChannelStatus().isReady() && rest11.getChannelStatus().isDone())
                    countB++;
                if (truck2.getChannelStatus().isReady() && rest12.getChannelStatus().isDone())
                    countB++;
                if (truck3.getChannelStatus().isReady() && rest13.getChannelStatus().isDone())
                    countB++;
                if (truck4.getChannelStatus().isReady() && rest14.getChannelStatus().isDone())
                    countB++;

                if (loader1.getChannelStatus().isBusy())
                    countB--;

                return countB > 0 && rest2.getChannelStatus().isDone();
            });

            loader1.setNext(connection3);
            loader2.setNext(connection3);

            connection3.addNext(truck1, () -> {
                return rest11.getChannelStatus().isDone();
            });

            connection3.addNext(truck2, () -> {
                return rest12.getChannelStatus().isDone();
            });

            connection3.addNext(truck3, () -> {
                return rest13.getChannelStatus().isDone();
            });

            connection3.addNext(truck4, () -> {
                return rest14.getChannelStatus().isDone();
            });

            truck1.setNext(connection4);
            truck2.setNext(connection4);
            truck3.setNext(connection4);
            truck4.setNext(connection4);

            Connection restCon1 = new Connection();
            rest1.setNext(restCon1);
            restCon1.addNext(rest1, () -> loader1.getChannelStatus().isDone());

            Connection restCon2 = new Connection();
            rest2.setNext(restCon2);
            restCon2.addNext(rest2, () -> loader2.getChannelStatus().isDone());

            Connection restCon11 = new Connection();
            rest11.setNext(restCon11);
            restCon11.addNext(rest11, () -> truck1.getChannelStatus().isDone());

            Connection restCon12 = new Connection();
            rest12.setNext(restCon12);
            restCon12.addNext(rest12, () -> truck2.getChannelStatus().isDone());

            Connection restCon13 = new Connection();
            rest13.setNext(restCon13);
            restCon13.addNext(rest13, () -> truck3.getChannelStatus().isDone());

            Connection restCon14 = new Connection();
            rest14.setNext(restCon14);
            restCon14.addNext(rest14, () -> truck4.getChannelStatus().isDone());

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


            var loader1St = loader1.getStats();
            var loader2St = loader2.getStats();

            var rest1St = rest1.getStats();
            var rest2St = rest2.getStats();

            var smoSt = smoWithQueue.getStats();

            var truck1St = truck1.getStats();
            var truck2St = truck2.getStats();
            var truck3St = truck3.getStats();
            var truck4St = truck4.getStats();
            var rest11St = rest11.getStats();
            var rest12St = rest12.getStats();
            var rest13St = rest13.getStats();
            var rest14St = rest14.getStats();

            Supplier<Integer> prod_served = () -> producer.getStats().getServed();

            Supplier<Double> mean_q_size = () -> smoSt.getAverageQSize() + smoSt.getBlockTime() / smoSt.getTotalSimTime();
            Supplier<Integer> max_q_size = () -> smoWithQueue.getStats().getMaxQSize();
            Supplier<Double> mean_wait_q = () -> (smoSt.getWaitQTime() + smoSt.getBlockTime()) / smoSt.getServed();
            

            Supplier<Integer> loader1_served = () -> loader1St.getServed();
            Supplier<Integer> loader2_served = () -> loader2St.getServed();
            Supplier<Double> mean_loader_q_size = () -> 2 - 
                (loader1St.getBusyTime() + loader2St.getBusyTime() + 
                rest1St.getBusyTime() + rest2St.getBusyTime()) / 
                loader1St.getTotalSimTime();
            Supplier<Double> mean_loader_wait_q = () -> mean_loader_q_size.get() * 
                loader1St.getTotalSimTime() / 
                (loader1St.getServed() + loader2St.getServed());

       
            Supplier<Double> mean_truck_q_size = () -> 4 -
                ((truck1St.getBusyTime() + truck2St.getBusyTime() + truck3St.getBusyTime() + truck4St.getBusyTime()) +
                (rest11St.getBusyTime() + rest12St.getBusyTime() + rest13St.getBusyTime() + rest14St.getBusyTime()) +
                (loader1St.getBusyTime() + loader2St.getBusyTime())) /
                (truck1St.getTotalSimTime());
            Supplier<Double> mean_truck_wait_q = () -> mean_truck_q_size.get() * 
                loader1St.getTotalSimTime() / 
                connection4.getOutputCount();

            Supplier<Double> productivity = () -> (double) connection4.getOutputCount() / truck1St.getTotalSimTime();
            Supplier<Double> processing_time = () -> (
                smoSt.getWaitQTime() + smoSt.getBlockTime() +
                loader1St.getBusyTime() +
                loader2St.getBusyTime() +
                truck1St.getBusyTime() +
                truck2St.getBusyTime() +
                truck3St.getBusyTime() +
                truck4St.getBusyTime()) / 
                connection4.getOutputCount();

            
            BaseResultCalculator calc = new BaseResultCalculator(
                prod_served,

                mean_q_size,
                max_q_size,
                mean_wait_q,
                
                loader1_served,
                loader2_served,
                mean_loader_q_size,
                mean_loader_wait_q,

                mean_truck_q_size,
                mean_truck_wait_q,

                productivity,
                processing_time
            );

            return Pair.createPair(model, calc);
        };
    }
}
