package com.safesupport.notify;

public record DeliveryResult(Status status, String target, String message) {

    public enum Status {
        SENT, SKIPPED, FAILED
    }

    public static DeliveryResult sent(String target) {
        return new DeliveryResult(Status.SENT, target, null);
    }

    public static DeliveryResult skipped(String target, String message) {
        return new DeliveryResult(Status.SKIPPED, target, message);
    }

    public static DeliveryResult failed(String target, String message) {
        return new DeliveryResult(Status.FAILED, target, message);
    }
}
