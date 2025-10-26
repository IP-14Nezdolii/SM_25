package com.example.modeling.components;

import java.util.List;
import java.util.Optional;

import com.example.modeling.components.device.Device;
import com.example.modeling.components.device.Device.DeviceRand;

public class CompDevice implements Component {
    private final Device device;
    private final String name;

    private Optional<Component> next = Optional.empty();

    protected int typ;

    public CompDevice(DeviceRand rand, String name) {
        this.name = name;
        this.device = new Device(rand, this.name);
    }

    public void setNext(Component next) {
        this.next = Optional.of(next);
    }

    @Override
    public void run(double time) {
        if (this.device.getWorkTime().isPresent()) {
            if(this.device.run(time)) {
                this.next.ifPresent(next -> next.process(this.typ));
            }
        } else {
            this.device.wait(time);
        }
    }

    @Override
    public boolean process(int typ) {
        if (this.device.getWorkTime().isPresent()) {
            return false;
        } else {
            this.typ = typ;
            this.device.process(typ);
            return true;
        }
    }

    @Override
    public Optional<Double> getWorkTime() {
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