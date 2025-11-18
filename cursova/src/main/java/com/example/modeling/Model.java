package com.example.modeling;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import org.decimal4j.immutable.Decimal6f;

import com.example.modeling.components.Component;
import com.example.modeling.components.Producer;
import com.example.modeling.components.Component.ComponentStats;

public class Model {
    private final ArrayList<Component> orderedElems = new ArrayList<>();
    private Decimal6f totalTime = Decimal6f.ZERO;

    public Model(Producer start) {
        addNextBFS(List.of(start));
    }

    public Model(List<Producer> producers) {
        addNextBFS(producers);
    }

    private void addNextBFS(List<Producer> producers) {
        Queue<Component> queue = new LinkedList<>();

        queue.addAll(producers);

        while (!queue.isEmpty()) {
            var elem = queue.poll();

            if (this.orderedElems.contains(elem) == false) {
                this.orderedElems.add(elem);

                queue.addAll(elem.getAllNext());
            }
        }
    }

    public void run(double runTime) {
        Decimal6f time = Decimal6f.valueOf(runTime);     
        Decimal6f currentTime = Decimal6f.ZERO;

        Optional<Decimal6f> t = getLeftTime();
        Decimal6f dt = t.get();

        while (dt.isLessThanOrEqualTo(time) && 
            currentTime.add(dt).isLessThanOrEqualTo(time)) {

            currentTime = currentTime.add(dt);
            this.changeState(dt);

            t = getLeftTime();
            if (t.isPresent()) {
                dt = t.get();
            } else {
                break;
            }
        }

        this.changeState(time.subtract(currentTime));
        this.totalTime = this.totalTime.add(time);
    }

    private void changeState(Decimal6f time) {
        if (time.isEqualTo(Decimal6f.ZERO)) {
            return;
        }

        for (var elem : orderedElems.reversed()) {
            elem.run(time);
        }
    }

    public Optional<Decimal6f> getLeftTime() {
        Decimal6f time = Decimal6f.MAX_VALUE;

        for (Component elem : this.orderedElems) {
            Optional<Decimal6f> elemTime = elem.getLeftTime();

            if (elemTime.isPresent()) {
                time = time.min(elemTime.get());
            }
        }

        return time != Decimal6f.MAX_VALUE
                ? Optional.of(time)
                : Optional.empty();
    }

    /*
     * Gathers statistics from all elements in the process
     */
    public ModelStats getStats() {
        ArrayList<Object> stats = new ArrayList<>();
        for (Component elem : this.orderedElems) {
            stats.add(elem.getStats());
        }
        return new ModelStats(stats, this.totalTime.doubleValue());
    }

    public class ModelStats {
        private final ArrayList<Object> elemStats;
        private final double totalTime;

        ModelStats(ArrayList<Object> elemStats, double totalTime) {
            this.elemStats = elemStats;
            this.totalTime = totalTime;
        }

        public double getTotalTime() {
            return this.totalTime;
        }

        @SuppressWarnings("unchecked")
        public ArrayList<ComponentStats> get() {
            return (ArrayList<ComponentStats>) this.elemStats.clone();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("[\n");

            for (Object stats : this.elemStats) {
                sb.append(" ").append(stats.toString()).append("\n");
            }

            sb.append("]\n");

            return sb.toString();
        }

        public void clear() {
            for (var stats : this.get()) {
                stats.clear();
            }
        }
    }

}
