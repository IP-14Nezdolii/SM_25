package com.example.modeling;

import java.util.Optional;

import com.example.modeling.components.Component;
import com.example.utils.DeviceRand.NextPriority;

import lombok.Getter;

public class Connection extends Component {
    private final Stats stats = new Stats();

    public Connection(NextPriority priority, String name) {
        super(priority, name);
    }

    @Override
    public void run(double time) {
        return;
    }

    @Override
    public boolean process() {
        stats.addRequest();

        if (this.getNextChosen().isPresent()) {
            return this.getNextChosen().get().process();
        }

        return true;
    }

    @Override
    public Optional<Double> getWorkTime() {
        return Optional.empty();
    }

    @Override
    public Object getStats() {
        return this.stats;
    }

    @Getter
    public class Stats {
        private long requestsNumber = 0;

        public void addRequest() {
            this.requestsNumber += 1;
        }

        public String toString() {
            return String.format(
                    "%s:{requests=%d}",
                    Connection.this.name,
                    this.requestsNumber
            );
        }
    }  
}