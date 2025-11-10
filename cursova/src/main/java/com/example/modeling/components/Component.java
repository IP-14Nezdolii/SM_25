package com.example.modeling.components;

import java.util.List;
import java.util.Optional;

import org.decimal4j.immutable.Decimal4f;

public interface Component {
    List<Component> getAllNext();
    Optional<Component> getNextChosen();

    Object getStats();
    String getName();

    Optional<Decimal4f> getLeftTime();

    void run(Decimal4f time);
    boolean process();
}
