package com.example.modeling.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.utils.DeviceRand.NextPriority;
import com.example.utils.Pair;

public abstract class Component {
    protected final ArrayList<Pair<Component, Long>> nextElems = new ArrayList<>();
    protected NextPriority priority;
    protected final String name;

    public Component(NextPriority priority, String name) {
        this.priority = priority;
        this.name = name;
    }

    public Component(String name) {
        this.priority = NextPriority.Priority;
        this.name = name;
    }

    public void addNext(Component elem, int score) {
        if (score <= 0) {
            throw new IllegalArgumentException("Score must be positive!");
        }

        this.nextElems.add(
                Pair.createPair(elem, Long.valueOf(score)));

        this.nextElems.sort((a, b) -> Long.compare(a.get1(), b.get1()));
    }

    public List<Component> getAllNext() {
        return this.nextElems.stream().map(p -> p.get0()).toList();
    }

    public String getName() {
        return this.name;
    }

    public void setNextPriority(NextPriority priority) {
        this.priority = priority;
    }

    /*
     * Returns NOT BUSY next element according to choosing method
     */
    public Optional<Component> getNextChosen() {
        List<Pair<Component, Long>> nexts = this.nextElems.stream()
                .filter(e -> e.get0().getWorkTime().isEmpty())
                .collect(Collectors.toList())
                .reversed();

        if (nexts.isEmpty()) {
            return Optional.empty();
            
        } else if (this.priority == NextPriority.Priority) {
            return Optional.of(nexts.getFirst().get0());

        } else if (this.priority == NextPriority.ProbabilityWithBusy || 
                    this.priority == NextPriority.Probability) {
            
            if(this.priority == NextPriority.ProbabilityWithBusy) {
                nexts = this.nextElems;
            } 

            Long total = nexts.stream()
                    .mapToLong(p -> p.get1().longValue())
                    .sum();

            double r = Math.random() * total;
            double cumulative = 0.0;

            for (Pair<Component, Long> p : nexts) {
                cumulative += p.get1();
                if (r <= cumulative) {
                    return Optional.of(p.get0());
                }
            }

            // Fallback (should not reach here)
            throw new IllegalStateException("Wrong algorithm!");
        } else {
            throw new IllegalStateException("Unknown choosing method");
        }
    }

    public abstract Optional<Double> getWorkTime();

    public abstract void run(double time);

    public abstract boolean process();

    public abstract Object getStats();
}
