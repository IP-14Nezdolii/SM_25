package com.example.modeling;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.example.modeling.components.Component;
import com.example.utils.DeviceRand.NextPriority;

import lombok.Getter;

public class Constraint<T extends Component> extends Component {
    private final Stats stats = new Stats();
    private final T wrappedElem;
    private final Predicate<T> predicate;

    private Optional<Component> ifFailure = Optional.empty();

    public Constraint(T wrappedElem, Predicate<T> predicate) {
        super(wrappedElem.priority, "constraint_"+ wrappedElem.name);

        this.wrappedElem = wrappedElem;
        this.predicate = predicate;
    }

    public Predicate<T> getPredicate() {
        return predicate;
    }

    public T getWrapped() {
        return this.wrappedElem;
    }

    public void setIfFailure(Component ifFailure) {
        this.ifFailure = Optional.of(ifFailure);
    }

    public Optional<Component> getIfFailure() {
        return this.ifFailure;
    }

    @Override
    public void addNext(Component elem, int score) {
        this.wrappedElem.addNext(elem, score);
    }

    @Override
    public List<Component> getAllNext() {
        return this.wrappedElem.getAllNext();
    }

    @Override
    public Optional<Component> getNextChosen() {
        return this.wrappedElem.getNextChosen();
    }

    @Override
    public void setNextPriority(NextPriority priority) {
        this.wrappedElem.setNextPriority(priority);
        this.priority = priority;
    }

    @Override
    public Optional<Double> getWorkTime() {
        return this.wrappedElem.getWorkTime();
    }

    @Override
    public void run(double time) {
        this.wrappedElem.run(time);
    }

    @Override
    public boolean process() {
        this.stats.addRequest();

        if (this.predicate.test(this.wrappedElem)) {
            return this.wrappedElem.process();
        } else {
            this.stats.addFailure();

            if (this.ifFailure.isPresent()) {
                this.ifFailure.get().process();
            }
            
            return false;
        }
    }

    @Override
    public Object getStats() {
        return List.of(this.stats, this.wrappedElem.getStats());
    }

    
    @Getter
    public class Stats {
        private long failures = 0;
        private long requestsNumber = 0;

        public void addRequest() {
            this.requestsNumber += 1;
        }

        public void addFailure() {
            this.failures += 1;
        }

        public double getFailureProbability() {
            return this.requestsNumber != 0.0
                ? (double)this.failures / (double)this.requestsNumber
                : 0.0;
        }

        public String toString() {
            return String.format(
                    "%s:{requests=%d, failures=%d, failureProbability=%.2f}",
                    Constraint.this.name,
                    this.requestsNumber,
                    this.failures,
                    this.getFailureProbability()
            );
        }
    } 
}
