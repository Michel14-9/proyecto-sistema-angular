package tracking.apollo.backendtracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendtrackingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendtrackingApplication.class, args);
        System.out.println("==============================================");
        System.out.println("  Microservicio de Tracking iniciado!");
        System.out.println("  Puerto: 8082");
        System.out.println("  BD: PostgreSQL luren_db (tabla tracking_delivery)");
        System.out.println("==============================================");
    }
}