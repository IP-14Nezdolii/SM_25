package com.example.modeling;

import java.util.List;

import com.example.modeling.utils.Status;

public class Producer extends SMO {

    public Producer(
        String name,
        List<Device> devices,
        int priority
    ) {
        super(name, 0, devices, priority);

        this.readyDevices.forEach((device) -> {
            device.process();

            this.stats.addRequest();
            this.busyDevices.add(device);
        });

        this.readyDevices.clear();
    }

    public Producer(
        String name,
        Device device,
        int priority
    ) {
        super(name, 0, List.of(device), priority);

        device.process();

        this.stats.addRequest();
        this.busyDevices.add(device);

        this.readyDevices.clear();
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Producer cannot process incoming tasks");
    }

    @Override
    public void handleEvents() {
        for (Device device : this.doneDevices) {
            device.setStatus(Status.READY);
            device.process();
            this.busyDevices.add(device);

            this.stats.addRequest();
            this.next.ifPresent((next) -> next.process());
        }

        this.doneDevices.clear();
    }
}
