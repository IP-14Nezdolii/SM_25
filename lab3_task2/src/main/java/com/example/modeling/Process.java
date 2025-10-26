package com.example.modeling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.example.modeling.components.Component;
import com.example.modeling.components.Constraint;
import com.example.modeling.components.Producer;

public class Process {
    private final Producer producer;

    private final ArrayList<Component> orderedElems = new ArrayList<>();

    public Process(Producer start, String name) {
        this.producer = start;
        recursiveCheckNext(
            this.producer, 
            new HashSet<Object>(), 
            new HashSet<Object>()
        );
    }

    private void recursiveCheckNext(Component elem, Set<Object> visited, Set<Object> saved) {
        if (visited.contains(elem)) {
            return;
        }
        visited.add(elem);

        if (elem instanceof Constraint) {
            Constraint constraint = (Constraint) elem;
            if (constraint.getIfFailure().isPresent()) {
                recursiveCheckNext((Component)constraint.getIfFailure().get(), visited, saved);
            }
        }

        if (!saved.contains(elem)) {
           this.orderedElems.add(elem);
           saved.add(elem);
        }
        

        for (Component next : elem.getAllNext()) {
            if (!saved.contains(next)) {
                this.orderedElems.add(next);
                saved.add(next);
            }
        }

        for (Component next : elem.getAllNext()) {
            recursiveCheckNext(next, visited, saved);
        }
    }

    public void run(double time) {  
        if (producer.getWorkTime().isEmpty()) {
            producer.process();
        }
        
        var t = getWorkTime();

        double currentTime = 0.0;
        double dt = t.get();

        while (currentTime + dt <= time) {

            currentTime += dt;
            this.runnAllElems(dt);
            
            t = getWorkTime();
            if (t.isPresent()) {
                dt = t.get();
            } else {
                break;
            }
        }

        runnAllElems(time - currentTime);
    }

    private void runnAllElems(double time) {
        for (Component elem : orderedElems) {
            elem.run(time);
        }
    }

    public Optional<Double> getWorkTime() {
        double time = Double.MAX_VALUE;

        for (Component elem : this.orderedElems) {
            Optional<Double> elemTime = elem.getWorkTime();

            if (elemTime.isPresent()) {
                time = Math.min(time, elemTime.get());
            }
        }

        return time != Double.MAX_VALUE 
            ? Optional.of(time) 
            : Optional.empty();
    }

    /*
     * Gathers statistics from all elements in the process
     */
    public ProcessStats getStats() {
        ArrayList<Object> stats = new ArrayList<>();
        for (Component elem : this.orderedElems) {
            stats.add(elem.getStats());
        }
        return new ProcessStats(stats);
    }

    public class ProcessStats {
        private final ArrayList<Object> elemStats;

        ProcessStats(ArrayList<Object> elemStats) {
            this.elemStats = elemStats;
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
    }

}
