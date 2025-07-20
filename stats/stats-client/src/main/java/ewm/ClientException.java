package ewm;

public class ClientException extends RuntimeException {

    public ClientException(int statusCode, String body) {
        super(String.format("Код ответа: %d, Тело Ответа: %s",statusCode,body));
    }
}
