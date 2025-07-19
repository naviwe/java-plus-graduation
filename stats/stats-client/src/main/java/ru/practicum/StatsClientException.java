package ru.practicum;



public class StatsClientException extends RuntimeException {

    public StatsClientException(int statusCode, String body) {
        super(String.format("Код ответа: %d, Тело Ответа: %s",statusCode,body));
    }
}
