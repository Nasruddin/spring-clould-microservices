package com.example;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.ribbon.proxy.annotation.Hystrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.EnableZuulServer;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.hateoas.Resources;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@IntegrationComponentScan
@EnableBinding(ReservationChannel.class)
@EnableFeignClients
@EnableDiscoveryClient
@EnableZuulProxy
@EnableCircuitBreaker
@SpringBootApplication
public class ReservationClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationClientApplication.class, args);
	}
}

interface ReservationChannel {

    @Output
    MessageChannel output();
}

@FeignClient("reservation-service")
interface ReservationReader {

    @GetMapping("/reservations")
    Resources<Reservation> read();
}

@MessagingGateway
interface ReservationWriter {
    @Gateway(requestChannel = "output")
    void write(String rn);
}


class Reservation {
    private String reservationName;

    public String getReservationName() {
        return reservationName;
    }

    public void setReservationName(String reservationName) {
        this.reservationName = reservationName;
    }
}

@RestController
@RequestMapping("/reservations")
class ReservationAPIGateway {

    private final ReservationReader reservationReader;
    private final ReservationWriter reservationWriter;

    @Autowired
    public ReservationAPIGateway(ReservationReader reservationReader, ReservationWriter reservationWriter) {
        this.reservationReader = reservationReader;
        this.reservationWriter = reservationWriter;
    }

    public Collection<String> fallback() {
        return new ArrayList<>();
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/names")
    public Collection<String> names() {
        return this.reservationReader
                .read()
                .getContent()
                .stream()
                .map(Reservation::getReservationName)
                .collect(Collectors.toList());
    }

    @PostMapping()
    public void write(@RequestBody Reservation reservation) {
        this.reservationWriter.write(reservation.getReservationName());
    }
}