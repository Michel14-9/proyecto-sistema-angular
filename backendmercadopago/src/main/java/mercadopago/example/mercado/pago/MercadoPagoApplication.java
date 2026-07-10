package mercadopago.example.mercado.pago;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MercadoPagoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MercadoPagoApplication.class, args);
        System.out.println(" Microservicio de Mercado Pago iniciado!");
        System.out.println(" Puerto: 8081");
    }
}