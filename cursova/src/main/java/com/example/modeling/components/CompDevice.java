package com.example.modeling.components;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.decimal4j.immutable.Decimal6f;

import com.example.modeling.components.device.Device;

public class CompDevice implements Component {
    private final Device device;
    private final String name;

    private Optional<Component> next = Optional.empty();

    public CompDevice(Supplier<Double> rand, String name) {
        this.name = name;
        this.device = new Device(rand, this.name);
    }

    public void setNext(Component next) {
        this.next = Optional.of(next);
    }

    @Override
    public void run(Decimal6f time) {
        if (this.device.getWorkTime().isPresent()) {

            if (time.isGreaterThan(this.device.getWorkTime().get())) {
                throw new IllegalArgumentException(
                    String.format(
                        "Invalid time %.4f: exceeds device work time %.4f",
                        time.doubleValue(),
                        this.device.getWorkTime().get().doubleValue()
                    )
                );
            }
            
            if(this.device.run(time)) {
                this.next.ifPresent(next -> next.process());
            }
        } else {
            this.device.wait(time);
        }
    }

    @Override
    public boolean process() {
        this.device.process();
        return true;
    }

    @Override
    public Optional<Decimal6f> getLeftTime() {
        return this.device.getWorkTime();
    }

    @Override
    public Device.Stats getStats() {
        return this.device.getStats();
    }

    @Override
    public List<Component> getAllNext() {
        return this.next.isPresent()
            ? List.of(this.next.get())
            : List.of();
    }

    @Override
    public Optional<Component> getNextChosen() {
        return next;
    }

    @Override
    public String getName() {
        return this.name;
    }
}