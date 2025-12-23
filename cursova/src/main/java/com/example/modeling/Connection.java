package com.example.modeling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.example.modeling.utils.Pair;
import com.example.modeling.utils.State;

public class Connection {
    private final Random rand = new Random();
    private final ArrayList<Pair<SingleChannelSMO, Supplier<Boolean>>> next = new ArrayList<>();

    private final int groupSize;
    private int size = 0;

    private int outputCount = 0;

    public Connection(int groupSize) {
        if (groupSize <= 0) {
            throw new IllegalArgumentException("Group size must be positive");
        }

        this.groupSize = groupSize;
    }

    public Connection() {
        this(1);
    }

    public void push() {
        if (this.next.isEmpty()) {
            this.size++;
            if (this.groupSize == this.size) {
                this.size = 0;
                this.outputCount++;
            }
            return;
        }

        List<SingleChannelSMO> lst = this.next.stream()
            .filter(elem -> elem.get0().getState().isReady() && elem.get1().get())
            .map(elem -> elem.get0())
            .toList(); 

        if (lst.isEmpty()) {
            throw new IllegalStateException("No available SMO to push the item");
        }

        this.size++;
        if (this.groupSize == this.size) {
            this.size = 0;
            lst.get(rand.nextInt(lst.size())).process();

            this.outputCount++;
        }
    }

    public void addNext(SingleChannelSMO smo) {
        if (smo == null) {
            throw new IllegalArgumentException("SMO must be not null");
        }

        this.next.add(Pair.createPair(smo, () -> true));
    }

    public void addNext(SingleChannelSMO smo, Supplier<Boolean> condition) {
        if (smo == null) {
            throw new IllegalArgumentException("SMO must be not null");
        }

        this.next.add(Pair.createPair(smo, condition));
    }

    public State getState() {
        return this.next.isEmpty()
            ? State.READY
            : this.next.stream()
                .anyMatch(elem -> elem.get0().getState().isReady() && elem.get1().get())
                        ? State.READY
                        : State.BUSY;
    }

    public int getOutputCount() {
        return this.outputCount;
    }
}
