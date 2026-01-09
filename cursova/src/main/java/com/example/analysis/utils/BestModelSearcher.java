package com.example.analysis.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.example.analysis.modelInit.ParamModelInitProducer;
import com.example.analysis.modelInit.ParamResultCalculator;
import com.example.modeling.Model;
import com.example.modeling.utils.Pair;

public class BestModelSearcher {
    private static final int MAX_PARAM_VALUE = 10;

    private final int N_SAMPLES;
    private final double TIME;
    private final double TRANS_PERIOD;

    private final Set<SimParams> visited = new HashSet<>(1000);
    private final ArrayList<Result> lst = new ArrayList<>(1000);
    private final Deque<SimParams> deque = new ArrayDeque<SimParams>(1000);
    
    public BestModelSearcher(
        int nSamples, 
        double time, 
        double transPeriod
    ) {
        this.N_SAMPLES = nSamples;
        this.TIME = time;
        this.TRANS_PERIOD = transPeriod;
    }

    public void findOptimalParams() {
        SimParams startParams1 = new SimParams(1, 1, 1, 4);
        SimParams startParams2 = new SimParams(1, 2, 0, 4);
        SimParams startParams3 = new SimParams(1, 0, 2, 4);
        
        if (!startParams1.isValid() || !startParams2.isValid() || !startParams3.isValid()) {
            throw new IllegalStateException("Start params are invalid!");
        } else {
            System.out.println("Starting optimization...");
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(8)){
            ExecutorCompletionService<Result> service = new ExecutorCompletionService<>(executor);
            int activeTasks = 0;

            this.visited.add(startParams1);
            this.deque.add(startParams1);

            this.visited.add(startParams2);
            this.deque.add(startParams2);

            this.visited.add(startParams3);
            this.deque.add(startParams3);

            while (!this.deque.isEmpty() || activeTasks > 0) {

                while (!this.deque.isEmpty() && activeTasks < 8) {
                    var p = this.deque.pop();

                    service.submit(()-> this.runSimulation(p));
                    activeTasks++;
                }

                if (activeTasks > 0) {
                    var result = service.take().get();
                    activeTasks--;

                    lst.add(result);
                    this.genNeighbors(result.params);
                }

                if (lst.size() % 10 == 0) {
                    System.out.println("State num: " + lst.size());
                }
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }

        lst.sort((var a, var b) -> Double.compare(a.getMaxQ(), b.getMaxQ()));
        System.out.println("Best 5:");
        for (int i = 0; i < 5; i++) {
            var r = lst.get(i);
            System.out.println(i + 1 + ". " + r);
        }

        lst.removeIf((var r) -> r.params.b != 1);
        System.out.println("Best 5 with Buldozers=1 :");
        for (int i = 0; i < 5; i++) {
            var r = lst.get(i);
            System.out.println(i + 1 + ". " + r);
        }
    }

    private void genNeighbors(SimParams p) {
        processNeighbor(new SimParams(p.b + 1, p.l1, p.l2, p.t));
        processNeighbor(new SimParams(p.b, p.l1 + 1, p.l2, p.t));
        processNeighbor(new SimParams(p.b, p.l1, p.l2 + 1, p.t));
        processNeighbor(new SimParams(p.b, p.l1, p.l2, p.t + 1));
    }

    private void processNeighbor(SimParams params) {
        if (params.isValid() && this.visited.add(params)) {
            this.deque.add(params);
        }
    }

    private Result runSimulation(SimParams p) {
        Supplier<Pair<Model, ParamResultCalculator>> init = 
            ParamModelInitProducer.getModelInit(p.b, p.l1, p.l2, p.t);

        double meanQ = 0;
        double meanLoaderQ = 0;
        double meanTruckQ = 0;

        for (int j = 0; j < N_SAMPLES; j++) {
            var model = init.get();

            model.get0().simulate(TRANS_PERIOD);
            model.get0().clearStats();  
            model.get0().simulate(TIME);
            
            var stats = model.get1();
            
            meanQ += stats.mean_q_size().get();
            meanLoaderQ += stats.mean_loader_q_size().get();
            meanTruckQ += stats.mean_truck_q_size().get();
        }

        meanQ /= this.N_SAMPLES;
        meanLoaderQ /= this.N_SAMPLES;
        meanTruckQ /= this.N_SAMPLES;
        
        return new Result(p, meanQ, meanLoaderQ, meanTruckQ);
    }

    public static class Result {
        final SimParams params;
        final double mean_q_size;
        final double mean_loader_q_size;
        final double mean_truck_q_size;

        public Result(
            SimParams params,
            double mean_q_size,
            double mean_loader_q_size,
            double mean_truck_q_size
        ) {
            this.params = params;
            this.mean_q_size = mean_q_size;
            this.mean_loader_q_size = mean_loader_q_size;
            this.mean_truck_q_size = mean_truck_q_size;
        }

        public double getMaxQ() {
            return Math.max(this.mean_q_size, Math.max(this.mean_loader_q_size, this.mean_truck_q_size));
        }

        @Override
        public String toString() {
            return "Params: " + params.toString() + 
                   "; Mean Q: " + mean_q_size + 
                   "; Mean Loader Q: " + mean_loader_q_size + 
                   "; Mean Truck Q: " + mean_truck_q_size;
        }
    }

    public static class SimParams {
        final int b;
        final int l1;
        final int l2;
        final int t;

        public SimParams(int b, int l1, int l2, int t) {
            this.b = b;
            this.l1 = l1;
            this.l2 = l2;
            this.t = t;
        }

        public boolean isValid() {
            if (t > MAX_PARAM_VALUE) return false;

            int sumL = l1 + l2;
            if (sumL > t) return false;
            if (b * 2 > sumL) return false;

            return true;
        }

        @Override
        public String toString() {
            return String.format("Buldozers=%d, Loaders1=%d, Loaders2=%d, Trucks=%d", b, l1, l2, t);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimParams simParams = (SimParams) o;
            return b == simParams.b && l1 == simParams.l1 && l2 == simParams.l2 && t == simParams.t;
        }

        @Override
        public int hashCode() {
            return Objects.hash(b, l1, l2, t);
        }
    }
}


