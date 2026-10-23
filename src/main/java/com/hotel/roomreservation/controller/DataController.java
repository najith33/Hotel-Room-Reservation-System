package com.hotel.roomreservation.controller;

import com.hotel.roomreservation.model.Room;
import com.hotel.roomreservation.model.RoomAvailability;
import com.hotel.roomreservation.repository.RoomRepository;
import com.hotel.roomreservation.repository.RoomAvailabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/data")
public class DataController {
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private RoomAvailabilityRepository availabilityRepository;
    
    @GetMapping("/populate-availability")
    public String populateAvailabilityData(RedirectAttributes redirectAttributes) {
        try {
            // Check if data already exists
            long existingSlots = availabilityRepository.count();
            if (existingSlots > 0) {
                redirectAttributes.addFlashAttribute("info", 
                    "Availability data already exists (" + existingSlots + " slots)");
                return "redirect:/guest/availability";
            }
            
            // Get all rooms
            List<Room> rooms = roomRepository.findAll();
            if (rooms.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No rooms found");
                return "redirect:/guest/availability";
            }
            
            // Create availability slots for the next 3 months
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusMonths(3);
            
            int totalSlots = 0;
            
            for (Room room : rooms) {
                // Create daily availability slots for each room
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    // Skip past dates
                    if (date.isBefore(LocalDate.now())) {
                        continue;
                    }
                    
                    // Create multiple time slots per day
                    createTimeSlotsForDate(room, date);
                    totalSlots += 4; // 4 slots per day
                }
            }
            
            redirectAttributes.addFlashAttribute("success", 
                "Successfully created " + totalSlots + " availability slots for " + rooms.size() + " rooms");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Failed to populate availability data: " + e.getMessage());
        }
        
        return "redirect:/guest/availability";
    }
    
    private void createTimeSlotsForDate(Room room, LocalDate date) {
        try {
            // Morning slot: 9:00 AM - 12:00 PM
            createAvailabilitySlot(room, date, LocalTime.of(9, 0), LocalTime.of(12, 0), "Morning slot");
            
            // Afternoon slot: 1:00 PM - 5:00 PM
            createAvailabilitySlot(room, date, LocalTime.of(13, 0), LocalTime.of(17, 0), "Afternoon slot");
            
            // Evening slot: 6:00 PM - 10:00 PM
            createAvailabilitySlot(room, date, LocalTime.of(18, 0), LocalTime.of(22, 0), "Evening slot");
            
            // Full day slot: 9:00 AM - 10:00 PM
            createAvailabilitySlot(room, date, LocalTime.of(9, 0), LocalTime.of(22, 0), "Full day slot");
            
        } catch (Exception e) {
            // Log error but continue
            System.err.println("Failed to create time slots for room " + room.getRoomId() + " on " + date + ": " + e.getMessage());
        }
    }
    
    private void createAvailabilitySlot(Room room, LocalDate date, LocalTime startTime, LocalTime endTime, String notes) {
        try {
            // Check if slot already exists
            List<RoomAvailability> existing = availabilityRepository.findByRoomAndDateRange(
                room.getRoomId(), date, date);
            
            boolean slotExists = existing.stream().anyMatch(slot -> 
                slot.getStartTime().equals(startTime) && slot.getEndTime().equals(endTime));
            
            if (slotExists) {
                return; // Skip if already exists
            }
            
            RoomAvailability availability = new RoomAvailability(room, date, startTime, endTime);
            availability.setNotes(notes);
            availability.setStatus(RoomAvailability.AvailabilityStatus.AVAILABLE);
            
            availabilityRepository.save(availability);
            
        } catch (Exception e) {
            System.err.println("Failed to create availability slot for room " + room.getRoomId() + " on " + date + ": " + e.getMessage());
        }
    }
}
