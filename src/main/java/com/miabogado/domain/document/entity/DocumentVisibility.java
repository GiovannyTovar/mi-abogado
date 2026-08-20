package com.miabogado.domain.document.entity;

public enum DocumentVisibility {
    /** Solo la firma. Es el valor por defecto: compartir es una decision explicita. */
    INTERNAL,
    /** Visible para el cliente en su portal. */
    SHARED_WITH_CLIENT
}
