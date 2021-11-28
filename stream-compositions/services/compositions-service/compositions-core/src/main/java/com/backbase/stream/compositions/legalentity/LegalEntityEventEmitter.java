package com.backbase.stream.compositions.legalentity;

public interface LegalEntityEventEmitter {
    void emitCompletedEvent();
    void emitFailedEvent();
}
