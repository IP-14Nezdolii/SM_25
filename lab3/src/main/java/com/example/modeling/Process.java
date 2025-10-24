package com.example.modeling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.example.modeling.components.Component;
import com.example.utils.DeviceRand.NextPriority;

public class Process extends Component {
    private final Component start;
    private final Connection end;
    private final ArrayList<Component> orderedElems = new ArrayList<>();

    public Process(
            Component start,
            String name,
            NextPriority priority) {
        this.start = start;
        this.end = new Connection(priority, name + "_output");
        super(priority, name);
        Construct();
    }

    public Process(
            Component start,
            String name) {
        this.start = start;
        this.end = new Connection(NextPriority.Priority, name + "_output");
        super(name);
        Construct();
    }

    private void Construct() {
        var visited = new HashSet<Object>();
            visited.add(this.end);

        recursiveCheckNext(this.start, visited);
        orderedElems.add(this.end);
    }

    @SuppressWarnings("rawtypes")
    private void recursiveCheckNext(Component elem, Set<Object> visited) {
        if (visited.contains(elem)) {
            return;
        }
        visited.add(elem);

        if (elem instanceof Constraint) {
            Constraint constraint = (Constraint) elem;
            if (constraint.getIfFailure().isPresent()) {
                recursiveCheckNext((Component)constraint.getIfFailure().get(), visited);
            }
        }

        this.orderedElems.add(elem);

        if (elem.getAllNext().size() == 0) {
            elem.addNext(this.end, 1);
        } else {
            for (Component next : elem.getAllNext()) {
                recursiveCheckNext(next, visited);
            }
        }
    }

    @Override
    public void run(double time) {    
        var t = getWorkTime();

        if (t.isEmpty()) {
            this.runnAllElems(time);
            return;
        }

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
        this.orderedElems.reversed().forEach(elem -> {
            elem.run(time);
        });
    }

    @Override
    public boolean process() {
        return this.start.process();
    }

    @Override
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
    @Override
    public Object getStats() {
        ArrayList<Object> stats = new ArrayList<>();
        for (Component elem : this.orderedElems) {
            stats.add(elem.getStats());
        }
        return new ProcessStats(stats);
    }

    @Override
    public void addNext(Component elem, int score) {
        this.end.addNext(elem, score);
    }

    @Override
    public void setNextPriority(NextPriority priority) {
        this.end.setNextPriority(priority);
        this.priority = priority;
    }

    @Override
    public List<Component> getAllNext() {
        return this.end.getAllNext();
    }

    @Override
    public Optional<Component> getNextChosen() {
        return this.end.getNextChosen();
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
