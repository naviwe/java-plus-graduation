package ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "ewm",
        "ewm.interaction.feign"
})
@EnableFeignClients(basePackages = {"ewm.interaction.feign","ewm.src.main.java.ewm"})
public class EventServiceApp {

    public static void main(String[] args) {

        SpringApplication.run(EventServiceApp.class,args);
    }
}