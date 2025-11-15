package com.example.modeling.components;

import java.util.List;
import java.util.Optional;

import org.decimal4j.immutable.Decimal6f;

public interface Component {
    List<Component> getAllNext();
    Optional<Component> getNextChosen();

    Object getStats();
    String getName();

    Optional<Decimal6f> getLeftTime();

    void run(Decimal6f time);
    boolean process();
}
