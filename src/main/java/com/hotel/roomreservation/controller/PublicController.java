package com.hotel.roomreservation.controller;

import com.hotel.roomreservation.model.Booking;
import com.hotel.roomreservation.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class PublicController {
    
    @Autowired
    private BookingService bookingService;
    
    @GetMapping("/verify-booking")
    public String verifyBooking(@RequestParam(required = false) String ref, Model model) {
        if (ref != null && !ref.trim().isEmpty()) {
            Optional<Booking> bookingOpt = bookingService.findByReferenceCodeWithDetails(ref);
            if (bookingOpt.isPresent()) {
                Booking booking = bookingOpt.get();
                model.addAttribute("booking", booking);
                model.addAttribute("found", true);
            } else {
                model.addAttribute("error", "Booking not found with reference code: " + ref);
                model.addAttribute("found", false);
            }
        } else {
            model.addAttribute("found", false);
        }
        return "public/verify-booking";
    }
    
    @GetMapping("/about")
    public String about() {
        return "public/about";
    }
    
    @GetMapping("/contact")
    public String contact() {
        return "public/contact";
    }
}
