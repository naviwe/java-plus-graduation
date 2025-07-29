package ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "ewm",
        "ewm.interaction"
})
@EnableFeignClients(basePackages = {"ewm.interaction.client","ewm.src.main.java"})
public class EventServiceApp {

    public static void main(String[] args) {

        SpringApplication.run(EventServiceApp.class,args);
    }
}