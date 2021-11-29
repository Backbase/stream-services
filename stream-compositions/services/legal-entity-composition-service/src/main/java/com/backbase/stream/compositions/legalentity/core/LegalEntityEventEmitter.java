package com.backbase.stream.compositions.legalentity.core;

public interface LegalEntityEventEmitter {
    void emitCompletedEvent();
    void emitFailedEvent();
}
