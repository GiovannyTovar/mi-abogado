package com.miabogado.domain.legalcase.entity;

public enum DeadlineStatus {
    PENDING,
    COMPLETED,
    /** Venció sin cumplirse. Lo marca el job diario; queda como registro, no se borra. */
    MISSED
}
