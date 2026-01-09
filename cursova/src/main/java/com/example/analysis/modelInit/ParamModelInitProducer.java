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

public class ParamModelInitProducer {

    public static Supplier<Pair<Model, ParamResultCalculator>> getModelInit(
        int buldozerNum, 
        int loader1Num, 
        int loader2Num,
        int truckNum
    ) {
        return () -> {
            ArrayList<SingleChannelSMO> list = new ArrayList<>();

            ArrayList<SingleChannelSMO> producerList = new ArrayList<>(buldozerNum);

            ArrayList<SingleChannelSMO> loader1List = new ArrayList<>(loader1Num);
            ArrayList<SingleChannelSMO> rest1List = new ArrayList<>(loader1Num);

            ArrayList<SingleChannelSMO> loader2List = new ArrayList<>(loader2Num);
            ArrayList<SingleChannelSMO> rest2List = new ArrayList<>(loader2Num);

            ArrayList<SingleChannelSMO> truckList = new ArrayList<>(truckNum);
            ArrayList<SingleChannelSMO> truckRestList = new ArrayList<>(truckNum);


            Connection connection1 = new Connection(2);
            Connection connection2 = new Connection();
            Connection connection3 = new Connection();
            Connection connection4 = new Connection();


            for (int i = 0; i < buldozerNum; i++) {
                var producer = new Producer(
                    "prod" + i, FunRand.getErlang(8,32), 1);

                producer.setNext(connection1);
                producerList.add(producer);
            }

            SingleChannelSMO smoWithQueue = new SingleChannelSMO(
                "q_smo", Integer.MAX_VALUE, FunRand.getFixed(0), 2);

            connection1.addNext(smoWithQueue);
            smoWithQueue.setNext(connection2);

            // Create loaders1 and rests
            for (int index = 0; index < loader1Num; index++) {
                var loader = new SingleChannelSMO(
                    "loader1" + index, FunRand.getExponential(14), 3);

                Connection restCon = new Connection();
                var rest = new SingleChannelSMO(
                    "rest1_" + index, FunRand.getFixed(5), 6);
                rest.setDoneStatus();

                connection2.addNext(loader, () -> {
                    int countB = 0;

                    for (int i = 0; i < truckList.size(); i++) {
                        var truck = truckList.get(i);
                        var truckRest = truckRestList.get(i);

                        if (truck.getChannelStatus().isReady() && 
                            truckRest.getChannelStatus().isDone())
                            countB++;
                    }

                    for (var l1 : loader1List) {
                        if (l1.getChannelStatus().isBusy() && l1 != loader)
                            countB--;
                    }

                    for (var l2 : loader2List) {
                        if (l2.getChannelStatus().isBusy())
                            countB--;
                    }

                    return countB > 0 && rest.getChannelStatus().isDone();
                });

                loader.setNext(connection3);

                rest.setNext(restCon);
                restCon.addNext(rest, () -> {
                    return loader.getChannelStatus().isDone();
                });

                loader1List.add(loader);
                rest1List.add(rest);
            }

            // Create loaders2 and rests
            for (int index = 0; index < loader2Num; index++) {
                var loader = new SingleChannelSMO(
                    "loader2" + index, FunRand.getExponential(12), 3);

                Connection restCon = new Connection();
                var rest = new SingleChannelSMO(
                    "rest2_" + index, FunRand.getFixed(5), 6);
                rest.setDoneStatus();

                connection2.addNext(loader, () -> {
                    int countB = 0;

                    for (int i = 0; i < truckList.size(); i++) {
                        var truck = truckList.get(i);
                        var truckRest = truckRestList.get(i);

                        if (truck.getChannelStatus().isReady() && 
                            truckRest.getChannelStatus().isDone())
                            countB++;
                    }

                    for (var l1 : loader1List) {
                        if (l1.getChannelStatus().isBusy())
                            countB--;
                    }

                    for (var l2 : loader2List) {
                        if (l2.getChannelStatus().isBusy() && l2 != loader)
                            countB--;
                    }

                    return countB > 0 && rest.getChannelStatus().isDone();
                });

                loader.setNext(connection3);

                rest.setNext(restCon);
                restCon.addNext(rest, () -> {
                    return loader.getChannelStatus().isDone();
                });

                loader2List.add(loader);
                rest2List.add(rest);
            }

            for (int index = 0; index < truckNum; index++) {
                var truck = new SingleChannelSMO("truck" + index, FunRand.getCombined(List.of(
                        FunRand.getNotNullNorm(22, 10),
                        FunRand.getUniform(2, 8)
                )), 4);

                Connection restCon = new Connection();
                var rest = new SingleChannelSMO(
                    "rest_truck_" + index, FunRand.getNotNullNorm(18, 10), 6);
                rest.setDoneStatus();

                connection3.addNext(truck, () -> {
                    return rest.getChannelStatus().isDone();
                });
                truck.setNext(connection4);

                rest.setNext(restCon);
                restCon.addNext(rest, () -> {
                    return truck.getChannelStatus().isDone();
                });
                
                truckList.add(truck);
                truckRestList.add(rest);
            }

            list.addAll(producerList);
            list.add(smoWithQueue);

            list.addAll(loader1List);
            list.addAll(rest1List);

            list.addAll(loader2List);
            list.addAll(rest2List);

            list.addAll(truckList);
            list.addAll(truckRestList);

            Model model = new Model(list);

            Supplier<Integer> prod_served = () -> {
                int total = 0;
                for (var p : producerList) {
                    total += p.getStats().getServed();
                }
                return total;
            };

            Supplier<Double> getTotalSimTime = () -> producerList.getFirst().getStats().getTotalSimTime();

            Supplier<Double> loaderBusyTime = () -> {
                double total = 0;
                for (var l : loader1List) {
                    total += l.getStats().getBusyTime();
                }
                for (var l : loader2List) {
                    total += l.getStats().getBusyTime();
                }
                return total;
            };

            Supplier<Integer> loaderServed = () -> {
                int total = 0;
                for (var l : loader1List) {
                    total += l.getStats().getServed();
                }
                for (var l : loader2List) {
                    total += l.getStats().getServed();
                }
                return total;
            };

            Supplier<Double> loaderRestBusyTime = () -> {
                double total = 0;
                for (var r : rest1List) {
                    total += r.getStats().getBusyTime();
                }
                for (var r : rest2List) {
                    total += r.getStats().getBusyTime();
                }
                return total;
            };

            Supplier<Double> truckBusyTime = () -> {
                double total = 0;
                for (var t : truckList) {
                    total += t.getStats().getBusyTime();
                }
                return total;
            };

            Supplier<Double> truckRestBusyTime = () -> {
                double total = 0;
                for (var r : truckRestList) {
                    total += r.getStats().getBusyTime();
                }
                return total;
            };

            var smoSt = smoWithQueue.getStats();
            Supplier<Double> mean_q_size = () -> smoSt.getAverageQSize() + smoSt.getBlockTime() / smoSt.getTotalSimTime();
            Supplier<Integer> max_q_size = () -> smoWithQueue.getStats().getMaxQSize();
            Supplier<Double> mean_wait_q = () -> (smoSt.getWaitQTime() + smoSt.getBlockTime()) / smoSt.getServed();
            
            Supplier<Double> mean_loader_q_size = () -> (loader1Num + loader2Num) - 
                (loaderBusyTime.get() + loaderRestBusyTime.get()) / getTotalSimTime.get();
            Supplier<Double> mean_loader_wait_q = () -> mean_loader_q_size.get() * 
                getTotalSimTime.get() / loaderServed.get();

            Supplier<Double> mean_truck_q_size = () -> truckNum -
                (truckBusyTime.get() + truckRestBusyTime.get() + loaderBusyTime.get()) / getTotalSimTime.get();
            Supplier<Double> mean_truck_wait_q = () -> mean_truck_q_size.get() * 
                smoSt.getTotalSimTime() / 
                connection4.getOutputCount();

            Supplier<Double> productivity = () -> (double) connection4.getOutputCount() / getTotalSimTime.get();
            Supplier<Double> processing_time = () -> (
                smoSt.getWaitQTime() + smoSt.getBlockTime() +
                loaderBusyTime.get() +
                truckBusyTime.get() +
                truckRestBusyTime.get()) / 
                connection4.getOutputCount();
            
            ParamResultCalculator calc = new ParamResultCalculator(
                buldozerNum,
                loader1Num,
                loader2Num,
                truckNum,

                prod_served,

                mean_q_size,
                max_q_size,
                mean_wait_q,
                
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

    public static void main( String[] args )
    {
        var p = getModelInit(1, 2, 0, 5).get();

        p.get0().simulate(16_000);
        p.get0().clearStats();
        p.get0().simulate(1440);

        var res = p.get1();
        System.out.println("Mean q size: " + res.mean_q_size().get());
        System.out.println("Mean loader q size: " + res.mean_loader_q_size().get());
        System.out.println("Mean truck q size: " + res.mean_truck_q_size().get());
        System.out.println("Productivity: " + res.productivity().get());

        p.get0().getStats().forEach((stats) -> System.out.println(stats));
    }
}
