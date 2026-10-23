package com.hotel.roomreservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HotelRoomReservationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelRoomReservationSystemApplication.class, args);
    }
}
