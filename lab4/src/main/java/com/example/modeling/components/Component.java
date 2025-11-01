package com.example.modeling.components;

import java.util.List;
import java.util.Optional;

public interface Component {

    List<Component> getAllNext();
    Optional<Component> getNextChosen();

    void setNext(Component next);

    Object getStats();
    String getName();

    
    Optional<Double> getWorkTime();
    void run(double time);
    boolean process();

    
}
