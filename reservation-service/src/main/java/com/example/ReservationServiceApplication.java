package com.example;

import com.netflix.discovery.converters.Auto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.stream.Stream;

@EnableBinding(ReservationChannel.class)
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}
}

@Component
class SampleDataCLR implements CommandLineRunner{

    private ReservationRepository reservationRepository;

    @Autowired
    public SampleDataCLR(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public void run(String... strings) throws Exception {
        Stream.of("Josh", "Juergen", "Andrew", "Bridget",
                "Onsi", "Phil", "Stephane", "Cornelia")
                .forEach(name -> reservationRepository.save(new Reservation(name)));

        reservationRepository.findAll().forEach(System.out::println);
    }
}

@RestResource
interface ReservationRepository extends JpaRepository<Reservation, Long> {

}

interface ReservationChannel {
    @Input
    SubscribableChannel input();
}

@MessageEndpoint
class ReservationProcessor {

    private ReservationRepository reservationRepository;

    @Autowired
    public ReservationProcessor(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @ServiceActivator(inputChannel = "input")
    public void onNewReservation(String reservationName) {
        this.reservationRepository.save(new Reservation(reservationName));
    }


}

@RestController
@RefreshScope
class MessageRestController {

    private final String value;

    @Autowired
    public MessageRestController(@Value("${message}") String value) {
        this.value = value;
    }

    @GetMapping("/message")
    String read() {
        return this.value;
    }
}

@Entity
class Reservation {

	@Id
	@GeneratedValue
	private Long id;

	private String reservationName;

	public Reservation() {
	}

    public Reservation(String reservationName) {
        this.reservationName = reservationName;
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getReservationName() {
		return reservationName;
	}

	public void setReservationName(String reservationName) {
		this.reservationName = reservationName;
	}

	@Override
	public String toString() {
		return "Reservation{" +
				"id=" + id +
				", reservationName='" + reservationName + '\'' +
				'}';
	}
}
