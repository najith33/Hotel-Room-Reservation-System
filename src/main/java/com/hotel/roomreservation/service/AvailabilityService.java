package com.hotel.roomreservation.service;

import com.hotel.roomreservation.model.Room;
import com.hotel.roomreservation.model.RoomAvailability;
import com.hotel.roomreservation.model.Booking;
import com.hotel.roomreservation.repository.RoomAvailabilityRepository;
import com.hotel.roomreservation.repository.RoomRepository;
import com.hotel.roomreservation.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AvailabilityService {
    
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityService.class);
    
    @Autowired
    private RoomAvailabilityRepository availabilityRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    /**
     * Create availability slots for a room
     */
    public RoomAvailability createAvailabilitySlot(Long roomId, LocalDate date, 
                                                   LocalTime startTime, LocalTime endTime, 
                                                   String notes) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new RuntimeException("Room not found");
        }
        
        Room room = roomOpt.get();
        
        // Check for conflicts
        List<RoomAvailability> conflicts = availabilityRepository.findConflictingAvailability(
            roomId, date, startTime, endTime, RoomAvailability.AvailabilityStatus.AVAILABLE);
        
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Time slot conflicts with existing availability");
        }
        
        RoomAvailability availability = new RoomAvailability(room, date, startTime, endTime);
        availability.setNotes(notes);
        availability.setStatus(RoomAvailability.AvailabilityStatus.BLOCKED); // Set as BLOCKED (not available)
        
        availability = availabilityRepository.save(availability);
        logger.info("Availability slot created (BLOCKED) for room {} on {}", roomId, date);
        return availability;
    }
    
    /**
     * Create multiple availability slots for a date range
     */
    public List<RoomAvailability> createAvailabilitySlots(Long roomId, LocalDate startDate, 
                                                           LocalDate endDate, LocalTime startTime, 
                                                           LocalTime endTime, String notes) {
        List<RoomAvailability> slots = new ArrayList<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            try {
                RoomAvailability slot = createAvailabilitySlot(roomId, date, startTime, endTime, notes);
                slots.add(slot);
            } catch (Exception e) {
                logger.warn("Failed to create slot for room {} on {}: {}", roomId, date, e.getMessage());
            }
        }
        
        return slots;
    }
    
    /**
     * Get availability by ID
     */
    public RoomAvailability getAvailabilityById(Long availabilityId) {
        Optional<RoomAvailability> availabilityOpt = availabilityRepository.findById(availabilityId);
        if (availabilityOpt.isEmpty()) {
            throw new RuntimeException("Availability not found");
        }
        return availabilityOpt.get();
    }
    
    /**
     * Update availability status
     */
    public RoomAvailability updateAvailabilityStatus(Long availabilityId, 
                                                     RoomAvailability.AvailabilityStatus status,
                                                     Long bookingId, String notes, String maintenanceReason) {
        Optional<RoomAvailability> availabilityOpt = availabilityRepository.findById(availabilityId);
        if (availabilityOpt.isEmpty()) {
            throw new RuntimeException("Availability not found");
        }
        
        RoomAvailability availability = availabilityOpt.get();
        availability.setStatus(status);
        availability.setBookingId(bookingId);
        availability.setNotes(notes);
        availability.setMaintenanceReason(maintenanceReason);
        
        availability = availabilityRepository.save(availability);
        logger.info("Availability status updated for slot {}", availabilityId);
        return availability;
    }
    
    /**
     * Block room for maintenance
     */
    public RoomAvailability blockForMaintenance(Long roomId, LocalDate date, 
                                                LocalTime startTime, LocalTime endTime, 
                                                String maintenanceReason, String notes) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new RuntimeException("Room not found");
        }
        
        Room room = roomOpt.get();
        RoomAvailability availability = new RoomAvailability(room, date, startTime, endTime, 
                                                              RoomAvailability.AvailabilityStatus.MAINTENANCE);
        availability.setMaintenanceReason(maintenanceReason);
        availability.setNotes(notes);
        
        availability = availabilityRepository.save(availability);
        logger.info("Room {} blocked for maintenance on {}", roomId, date);
        return availability;
    }
    
    /**
     * Get availability for a specific room and date
     */
    public List<RoomAvailability> getRoomAvailability(Long roomId, LocalDate date) {
        return availabilityRepository.findByRoomRoomIdAndDateAndStatus(
            roomId, date, RoomAvailability.AvailabilityStatus.AVAILABLE);
    }
    
    /**
     * Get availability for a date range
     */
    public List<RoomAvailability> getRoomAvailabilityRange(Long roomId, LocalDate startDate, LocalDate endDate) {
        return availabilityRepository.findByRoomAndDateRange(roomId, startDate, endDate);
    }
    
    /**
     * Get all available rooms for a specific date
     */
    public List<RoomAvailability> getAvailableRoomsForDate(LocalDate date) {
        return availabilityRepository.findByDateAndStatus(date, RoomAvailability.AvailabilityStatus.AVAILABLE);
    }
    
    /**
     * Get all room availability for a specific date (all statuses)
     */
    public List<RoomAvailability> getAllRoomAvailabilityForDate(LocalDate date) {
        return availabilityRepository.findByDate(date);
    }
    
    /**
     * Get available rooms by type for a specific date
     */
    public List<RoomAvailability> getAvailableRoomsByTypeForDate(Room.RoomType roomType, LocalDate date) {
        return availabilityRepository.findByRoomTypeAndDateAndStatus(
            roomType, date, RoomAvailability.AvailabilityStatus.AVAILABLE);
    }
    
    /**
     * Get calendar view data for a month
     */
    public Map<String, Object> getCalendarData(LocalDate month, Long roomId) {
        LocalDate startOfMonth = month.withDayOfMonth(1);
        LocalDate endOfMonth = month.withDayOfMonth(month.lengthOfMonth());
        
        List<RoomAvailability> availabilities;
        if (roomId != null) {
            availabilities = getRoomAvailabilityRange(roomId, startOfMonth, endOfMonth);
        } else {
            availabilities = new ArrayList<>();
            for (LocalDate date = startOfMonth; !date.isAfter(endOfMonth); date = date.plusDays(1)) {
                // Get ALL availability slots for each date, not just available ones
                availabilities.addAll(getAllRoomAvailabilityForDate(date));
            }
        }
        
        // Filter to show only UNAVAILABLE slots (booked, maintenance, blocked)
        // This is for management view - we don't want to see available slots
        availabilities = availabilities.stream()
            .filter(availability -> availability.getStatus() != RoomAvailability.AvailabilityStatus.AVAILABLE)
            .collect(Collectors.toList());
        
        logger.info("Retrieved {} unavailable slots for month {} (filtered out available slots)", availabilities.size(), month);
        
        // Debug: Log each slot that will be displayed
        for (RoomAvailability availability : availabilities) {
            logger.info("Slot: {} - {} {} {} (Status: {})", 
                availability.getAvailabilityId(),
                availability.getDate(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.getStatus());
        }
        
        // Load booking details for booked slots
        Map<Long, Booking> bookingMap = new HashMap<>();
        for (RoomAvailability availability : availabilities) {
            if (availability.getStatus() == RoomAvailability.AvailabilityStatus.BOOKED && 
                availability.getBookingId() != null) {
                Optional<Booking> bookingOpt = bookingRepository.findById(availability.getBookingId());
                if (bookingOpt.isPresent()) {
                    bookingMap.put(availability.getAvailabilityId(), bookingOpt.get());
                }
            }
        }
        
        Map<String, Object> calendarData = new HashMap<>();
        calendarData.put("month", month);
        calendarData.put("startOfMonth", startOfMonth);
        calendarData.put("endOfMonth", endOfMonth);
        calendarData.put("availabilities", availabilities);
        calendarData.put("bookingMap", bookingMap);
        
        // Group by date for easier frontend processing
        Map<LocalDate, List<RoomAvailability>> groupedByDate = availabilities.stream()
            .collect(Collectors.groupingBy(RoomAvailability::getDate));
        calendarData.put("groupedByDate", groupedByDate);
        
        return calendarData;
    }
    
    /**
     * Get availability summary for dashboard
     */
    public Map<String, Object> getAvailabilitySummary() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusWeeks(1);
        
        Map<String, Object> summary = new HashMap<>();
        
        // Count available slots today
        long availableToday = getAvailableRoomsForDate(today).size();
        summary.put("availableToday", availableToday);
        
        // Count total slots today
        long totalSlotsToday = availabilityRepository.findByDateAndStatus(today, RoomAvailability.AvailabilityStatus.AVAILABLE).size() +
                              availabilityRepository.findByDateAndStatus(today, RoomAvailability.AvailabilityStatus.BOOKED).size() +
                              availabilityRepository.findByDateAndStatus(today, RoomAvailability.AvailabilityStatus.MAINTENANCE).size();
        summary.put("totalSlotsToday", totalSlotsToday);
        
        // Count maintenance slots
        long maintenanceSlots = availabilityRepository.findByDateAndStatus(today, RoomAvailability.AvailabilityStatus.MAINTENANCE).size();
        summary.put("maintenanceSlots", maintenanceSlots);
        
        // Get upcoming bookings
        List<RoomAvailability> upcomingBookings = new ArrayList<>();
        for (LocalDate date = today; !date.isAfter(nextWeek); date = date.plusDays(1)) {
            upcomingBookings.addAll(availabilityRepository.findByDateAndStatus(date, RoomAvailability.AvailabilityStatus.BOOKED));
        }
        summary.put("upcomingBookings", upcomingBookings.size());
        
        return summary;
    }
    
    /**
     * Delete availability slot
     */
    public void deleteAvailability(Long availabilityId) {
        Optional<RoomAvailability> availabilityOpt = availabilityRepository.findById(availabilityId);
        if (availabilityOpt.isEmpty()) {
            throw new RuntimeException("Availability not found");
        }
        
        RoomAvailability availability = availabilityOpt.get();
        
        // Only allow deletion of available or blocked slots, not booked ones
        if (availability.getStatus() == RoomAvailability.AvailabilityStatus.BOOKED) {
            throw new RuntimeException("Cannot delete booked availability slots");
        }
        
        availabilityRepository.deleteById(availabilityId);
        logger.info("Availability slot deleted: {}", availabilityId);
    }
    
    /**
     * Bulk delete availability slots
     */
    public int bulkDeleteAvailability(List<Long> availabilityIds) {
        int deletedCount = 0;
        for (Long id : availabilityIds) {
            try {
                deleteAvailability(id);
                deletedCount++;
            } catch (Exception e) {
                logger.warn("Failed to delete availability {}: {}", id, e.getMessage());
            }
        }
        return deletedCount;
    }
    
    
    /**
     * Search availability by criteria
     */
    public List<RoomAvailability> searchAvailability(Long roomId, LocalDate startDate, 
                                                     LocalDate endDate, RoomAvailability.AvailabilityStatus status) {
        if (roomId != null && startDate != null && endDate != null) {
            return availabilityRepository.findByRoomAndDateRange(roomId, startDate, endDate)
                .stream()
                .filter(av -> status == null || av.getStatus() == status)
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
