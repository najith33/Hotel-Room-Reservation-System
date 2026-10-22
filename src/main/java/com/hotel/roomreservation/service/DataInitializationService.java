package com.hotel.roomreservation.service;

import com.hotel.roomreservation.model.Room;
import com.hotel.roomreservation.model.RoomAvailability;
import com.hotel.roomreservation.repository.RoomRepository;
import com.hotel.roomreservation.repository.RoomAvailabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DataInitializationService implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializationService.class);
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private RoomAvailabilityRepository availabilityRepository;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // DISABLED: Auto-creation of availability slots
        // initializeAvailabilityData();
        logger.info("DataInitializationService: Auto-creation of availability slots is DISABLED");
    }
    
    private void initializeAvailabilityData() {
        try {
            // Check if data already exists
            long existingSlots = availabilityRepository.count();
            if (existingSlots > 0) {
                logger.info("Availability data already exists ({} slots), skipping initialization", existingSlots);
                return;
            }
            
            logger.info("Initializing availability data...");
            
            // Get all rooms
            List<Room> rooms = roomRepository.findAll();
            if (rooms.isEmpty()) {
                logger.warn("No rooms found, cannot initialize availability data");
                return;
            }
            
            // Create availability slots for the next 3 months
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusMonths(3);
            
            int totalSlots = 0;
            
            for (Room room : rooms) {
                logger.info("Creating availability slots for room: {}", room.getRoomName());
                
                // Create daily availability slots for each room
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    // Skip past dates
                    if (date.isBefore(LocalDate.now())) {
                        continue;
                    }
                    
                    // Create multiple time slots per day
                    createTimeSlotsForDate(room, date);
                    totalSlots++;
                }
            }
            
            logger.info("Successfully initialized {} availability slots for {} rooms", totalSlots, rooms.size());
            
        } catch (Exception e) {
            logger.error("Error initializing availability data", e);
        }
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
            logger.warn("Failed to create time slots for room {} on {}: {}", room.getRoomId(), date, e.getMessage());
        }
    }
    
    private void createAvailabilitySlot(Room room, LocalDate date, LocalTime startTime, LocalTime endTime, String notes) {
        try {
            // Check if slot already exists by querying all slots for this room and date
            List<RoomAvailability> existing = availabilityRepository.findByRoomAndDateRange(
                room.getRoomId(), date, date);
            
            boolean slotExists = existing.stream().anyMatch(slot -> 
                slot.getStartTime().equals(startTime) && slot.getEndTime().equals(endTime));
            
            if (slotExists) {
                logger.debug("Availability slot already exists for room {} on {} at {}", 
                    room.getRoomId(), date, startTime);
                return;
            }
            
            RoomAvailability availability = new RoomAvailability(room, date, startTime, endTime);
            availability.setNotes(notes);
            availability.setStatus(RoomAvailability.AvailabilityStatus.AVAILABLE);
            
            availabilityRepository.save(availability);
            logger.debug("Created availability slot for room {} on {} at {}", 
                room.getRoomId(), date, startTime);
                
        } catch (Exception e) {
            logger.warn("Failed to create availability slot for room {} on {}: {}", 
                room.getRoomId(), date, e.getMessage());
        }
    }
}
