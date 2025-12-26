package com.example.verification;

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

    public static Supplier<Pair<Model, ResultCalculator>> getModelInit(Supplier<Double>[] params) {
        return () -> {
            ArrayList<SingleChannelSMO> list = new ArrayList<>();

            SingleChannelSMO producer = new Producer(
                    "prod1", params[0], 1);

            SingleChannelSMO smoWithQueue = new SingleChannelSMO(
                    "q_smo", Integer.MAX_VALUE, FunRand.getFixed(0), 2);

            SingleChannelSMO loader1 = new SingleChannelSMO(
                    "loader1", params[1], 3);
            SingleChannelSMO loader2 = new SingleChannelSMO(
                    "loader2", params[2], 3);

            SingleChannelSMO truck1 = new SingleChannelSMO(
                    "truck1", FunRand.getCombined(List.of(
                            params[3],
                            params[4])),
                    4);

            SingleChannelSMO truck2 = new SingleChannelSMO(
                    "truck2", FunRand.getCombined(List.of(
                            params[3],
                            params[4])),
                    4);

            SingleChannelSMO truck3 = new SingleChannelSMO(
                    "truck3", FunRand.getCombined(List.of(
                            params[3],
                            params[4])),
                    4);

            SingleChannelSMO truck4 = new SingleChannelSMO(
                    "truck4", FunRand.getCombined(List.of(
                            params[3],
                            params[4])),
                    4);

            SingleChannelSMO rest1 = new SingleChannelSMO(
                    "rest1", params[5], 5);
            rest1.setDoneStatus();

            SingleChannelSMO rest2 = new SingleChannelSMO(
                    "rest2", params[5], 5);
            rest2.setDoneStatus();

            SingleChannelSMO rest11 = new SingleChannelSMO(
                    "rest11", params[6], 6);
            rest11.setDoneStatus();

            SingleChannelSMO rest12 = new SingleChannelSMO(
                    "rest12", params[6], 6);
            rest12.setDoneStatus();

            SingleChannelSMO rest13 = new SingleChannelSMO(
                    "rest13", params[6], 6);
            rest13.setDoneStatus();

            SingleChannelSMO rest14 = new SingleChannelSMO(
                    "rest14", params[6], 6);
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

                if (truck1.getChannelState().isReady() && rest11.getChannelState().isDone())
                    countB++;
                if (truck2.getChannelState().isReady() && rest12.getChannelState().isDone())
                    countB++;
                if (truck3.getChannelState().isReady() && rest13.getChannelState().isDone())
                    countB++;
                if (truck4.getChannelState().isReady() && rest14.getChannelState().isDone())
                    countB++;

                if (loader2.getChannelState().isBusy())
                    countB--;

                return countB > 0 && rest1.getChannelState().isDone();
            });

            connection2.addNext(loader2, () -> {
                int countB = 0;

                if (truck1.getChannelState().isReady() && rest11.getChannelState().isDone())
                    countB++;
                if (truck2.getChannelState().isReady() && rest12.getChannelState().isDone())
                    countB++;
                if (truck3.getChannelState().isReady() && rest13.getChannelState().isDone())
                    countB++;
                if (truck4.getChannelState().isReady() && rest14.getChannelState().isDone())
                    countB++;

                if (loader1.getChannelState().isBusy())
                    countB--;

                return countB > 0 && rest2.getChannelState().isDone();
            });

            loader1.setNext(connection3);
            loader2.setNext(connection3);

            connection3.addNext(truck1, () -> {
                return rest11.getChannelState().isDone();
            });

            connection3.addNext(truck2, () -> {
                return rest12.getChannelState().isDone();
            });

            connection3.addNext(truck3, () -> {
                return rest13.getChannelState().isDone();
            });

            connection3.addNext(truck4, () -> {
                return rest14.getChannelState().isDone();
            });

            truck1.setNext(connection4);
            truck2.setNext(connection4);
            truck3.setNext(connection4);
            truck4.setNext(connection4);

            Connection restCon1 = new Connection();
            rest1.setNext(restCon1);
            restCon1.addNext(rest1, () -> loader1.getChannelState().isDone());

            Connection restCon2 = new Connection();
            rest2.setNext(restCon2);
            restCon2.addNext(rest2, () -> loader2.getChannelState().isDone());

            Connection restCon11 = new Connection();
            rest11.setNext(restCon11);
            restCon11.addNext(rest11, () -> truck1.getChannelState().isDone());

            Connection restCon12 = new Connection();
            rest12.setNext(restCon12);
            restCon12.addNext(rest12, () -> truck2.getChannelState().isDone());

            Connection restCon13 = new Connection();
            rest13.setNext(restCon13);
            restCon13.addNext(rest13, () -> truck3.getChannelState().isDone());

            Connection restCon14 = new Connection();
            rest14.setNext(restCon14);
            restCon14.addNext(rest14, () -> truck4.getChannelState().isDone());

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

            Supplier<Long> prod_served = () -> producer.getStats().getServed();

            Supplier<Double> mean_q_size = () -> smoSt.getAverageQueueSize() + smoSt.getBlockTime() / smoSt.getTotalSimTime();
            Supplier<Double> mean_wait_q = () -> (smoSt.getWaitTime() + smoSt.getBlockTime()) / smoSt.getServed();
            Supplier<Long> q_served = () -> smoWithQueue.getStats().getServed();

            Supplier<Double> m_loader_util = () -> (
                loader1St.getBusyTime() + loader2St.getBusyTime() + 
                rest1St.getBusyTime() + rest2St.getBusyTime()) /
                2 / loader1St.getTotalSimTime();
            Supplier<Double> mean_loader_q_size = () -> m_loader_util.get() * 2;
            Supplier<Double> mean_loader_wait_q = () -> mean_loader_q_size.get() * 
                loader1St.getTotalSimTime() / 
                (loader1St.getServed() + loader2St.getServed());

            Supplier<Double> m_truck_util = () -> ((truck1St.getBusyTime() + truck2St.getBusyTime() + truck3St.getBusyTime() + truck4St.getBusyTime()) +
                        (rest11St.getBusyTime() + rest12St.getBusyTime() + rest13St.getBusyTime() + rest14St.getBusyTime()) +
                        (loader1St.getBusyTime() + loader2St.getBusyTime())) /
                        4 / (truck1St.getTotalSimTime());
            Supplier<Double> mean_truck_q_size = () -> (1.0 - m_truck_util.get()) * 4;
            Supplier<Double> mean_truck_wait_q = () -> mean_truck_q_size.get() * 
                loader1St.getTotalSimTime() / 
                connection4.getOutputCount();

            Supplier<Double> productivity = () -> (double) connection4.getOutputCount() / truck1St.getTotalSimTime();
            Supplier<Double> processing_time = () -> (
                smoSt.getWaitTime() + smoSt.getBlockTime() +
                loader1St.getBusyTime() +
                loader2St.getBusyTime() +
                truck1St.getBusyTime() +
                truck2St.getBusyTime() +
                truck3St.getBusyTime() +
                truck4St.getBusyTime()) / 
                connection4.getOutputCount();

            
            ResultCalculator calc = new ResultCalculator(
                prod_served,

                mean_q_size,
                mean_wait_q,
                q_served,

                m_loader_util,
                mean_loader_q_size,
                mean_loader_wait_q,

                m_truck_util,
                mean_truck_q_size,
                mean_truck_wait_q,

                productivity,
                processing_time
            );

            return Pair.createPair(model, calc);
        };
    }
}
