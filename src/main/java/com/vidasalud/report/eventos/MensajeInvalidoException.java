package com.vidasalud.report.eventos;

/** El evento llegó sin los datos mínimos (p. ej. sin citaId). Reintentar no lo arregla: va directo al DLT. */
public class MensajeInvalidoException extends RuntimeException {

    public MensajeInvalidoException(String mensaje) {
        super(mensaje);
    }
}
