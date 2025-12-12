package com.example.modeling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.example.modeling.utils.Pair;
import com.example.modeling.utils.Status;

public class Connection {
    private final Random rand = new Random();
    private final ArrayList<Pair<SMO, Supplier<Boolean>>> next = new ArrayList<>();

    private final int groupSize;
    private int size = 0;

    public Connection(int groupSize) {
        if (groupSize <= 0) {
            throw new IllegalArgumentException("Group size must be positive");
        }

        this.groupSize = groupSize;
    }

    public Connection() {
        this.groupSize = 1;
    }

    public void process() {
        List<SMO> lst = this.next.stream()
            .filter(elem -> elem.get0().getStatus() == Status.READY && elem.get1().get())
            .map(elem -> elem.get0())
            .toList(); 

        if (lst.isEmpty()) {
            throw new IllegalStateException("No SMO is ready to process the task");
        }

        this.size++;
        if (this.groupSize == this.size) {
            this.size = 0;
            lst.get(rand.nextInt(lst.size())).process();
        }
    }

    public void addNext(SMO smo) {
        if (smo == null) {
            throw new IllegalArgumentException("SMO must be not null");
        }

        this.next.add(Pair.createPair(smo, () -> true));
    }

    public void addNext(SMO smo, Supplier<Boolean> condition) {
        if (smo == null) {
            throw new IllegalArgumentException("SMO must be not null");
        }

        this.next.add(Pair.createPair(smo, condition));
    }

    public Status getStatus() {
        return this.next.stream()
                .anyMatch(elem -> elem.get0().getStatus() == Status.READY && elem.get1().get())
                        ? Status.READY
                        : Status.BUSY;
    }
}
