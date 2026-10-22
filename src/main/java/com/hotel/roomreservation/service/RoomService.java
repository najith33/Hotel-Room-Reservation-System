package com.hotel.roomreservation.service;

import com.hotel.roomreservation.model.Room;
import com.hotel.roomreservation.model.RoomAvailability;
import com.hotel.roomreservation.repository.RoomAvailabilityRepository;
import com.hotel.roomreservation.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RoomService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private RoomAvailabilityRepository roomAvailabilityRepository;
    
    /**
     * Create a new room
     */
    public Room createRoom(String roomName, Room.RoomType roomType, Integer capacity, 
                            BigDecimal hourlyRate, String description) {
        // Validate input parameters
        if (roomName == null || roomName.trim().isEmpty()) {
            throw new IllegalArgumentException("Room name cannot be null or empty");
        }
        if (roomType == null) {
            throw new IllegalArgumentException("Room type cannot be null");
        }
        if (capacity == null || capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Hourly rate must be greater than 0");
        }
        
        try {
            Room room = new Room(roomName, roomType, capacity, hourlyRate);
            room.setDescription(description);
            
            room = roomRepository.save(room);
            logger.info("Room created successfully: {}", roomName);
            return room;
        } catch (Exception e) {
            logger.error("Error creating room: {}", roomName, e);
            throw e;
        }
    }
    
    /**
     * Update room information
     */
    public Room updateRoom(Long roomId, String roomName, Integer capacity, 
                            BigDecimal hourlyRate, String description) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new RuntimeException("Room not found");
        }
        
        Room room = roomOpt.get();
        room.setRoomName(roomName);
        room.setCapacity(capacity);
        room.setHourlyRate(hourlyRate);
        room.setDescription(description);
        
        room = roomRepository.save(room);
        logger.info("Room updated successfully: {}", roomName);
        return room;
    }
    
    /**
     * Deactivate room
     */
    public Room deactivateRoom(Long roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new RuntimeException("Room not found");
        }
        
        Room room = roomOpt.get();
        room.setIsActive(false);
        
        room = roomRepository.save(room);
        logger.info("Room deactivated: {}", room.getRoomName());
        return room;
    }
    
    /**
     * Activate room
     */
    public Room activateRoom(Long roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            throw new RuntimeException("Room not found");
        }
        
        Room room = roomOpt.get();
        room.setIsActive(true);
        
        room = roomRepository.save(room);
        logger.info("Room activated: {}", room.getRoomName());
        return room;
    }
    
    /**
     * Get all rooms, including inactive ones (used by manager administration).
     */
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    /**
     * Get all active rooms
     */
    public List<Room> getAllActiveRooms() {
        return roomRepository.findByIsActiveTrue();
    }
    
    /**
     * Get rooms by type
     */
    public List<Room> getRoomsByType(Room.RoomType roomType) {
        return roomRepository.findByRoomTypeAndIsActiveTrue(roomType);
    }
    
    /**
     * Get rooms by capacity
     */
    public List<Room> getRoomsByCapacity(Integer minCapacity) {
        return roomRepository.findByCapacityGreaterThanEqualAndIsActiveTrue(minCapacity);
    }
    
    /**
     * Search rooms by name
     */
    public List<Room> searchRoomsByName(String name) {
        return roomRepository.findByRoomNameContainingAndIsActiveTrue(name);
    }
    
    /**
     * Get rooms by price range
     */
    public List<Room> getRoomsByPriceRange(Double maxRate) {
        return roomRepository.findByHourlyRateLessThanEqualAndIsActiveTrueOrderByHourlyRateAsc(maxRate);
    }
    
    /**
     * Find room by ID
     */
    public Optional<Room> findById(Long roomId) {
        return roomRepository.findById(roomId);
    }
    
    /**
     * Check room availability for specific date and time
     */
    public List<RoomAvailability> getRoomAvailability(Long roomId, LocalDate date) {
        return roomAvailabilityRepository.findByRoomRoomIdAndDateAndStatus(roomId, date, RoomAvailability.AvailabilityStatus.AVAILABLE);
    }
    
    /**
     * Get room availability for date range
     */
    public List<RoomAvailability> getRoomAvailabilityRange(Long roomId, LocalDate startDate, LocalDate endDate) {
        return roomAvailabilityRepository.findByRoomAndDateRange(roomId, startDate, endDate);
    }
    
    /**
     * Get all available rooms for specific date
     */
    public List<RoomAvailability> getAvailableRoomsForDate(LocalDate date) {
        return roomAvailabilityRepository.findByDateAndStatus(date, RoomAvailability.AvailabilityStatus.AVAILABLE);
    }
    
    /**
     * Get available rooms by type for specific date
     */
    public List<RoomAvailability> getAvailableRoomsByTypeForDate(Room.RoomType roomType, LocalDate date) {
        return roomAvailabilityRepository.findByRoomTypeAndDateAndStatus(roomType, date, RoomAvailability.AvailabilityStatus.AVAILABLE);
    }
    
    /**
     * Create room availability slots
     */
    public RoomAvailability createAvailabilitySlot(Long roomId, LocalDate date, 
                                                   LocalTime startTime, LocalTime endTime) {
        RoomAvailability availability = new RoomAvailability();
        Room room = new Room();
        room.setRoomId(roomId);
        availability.setRoom(room);
        availability.setDate(date);
        availability.setStartTime(startTime);
        availability.setEndTime(endTime);
        availability.setIsAvailable(true);
        
        availability = roomAvailabilityRepository.save(availability);
        logger.info("Availability slot created for room {} on {}", roomId, date);
        return availability;
    }
    
    /**
     * Update room availability
     */
    public RoomAvailability updateAvailability(Long availabilityId, boolean isAvailable, Long bookingId) {
        Optional<RoomAvailability> availabilityOpt = roomAvailabilityRepository.findById(availabilityId);
        if (availabilityOpt.isEmpty()) {
            throw new RuntimeException("Availability not found");
        }
        
        RoomAvailability availability = availabilityOpt.get();
        availability.setIsAvailable(isAvailable);
        availability.setBookingId(bookingId);
        
        availability = roomAvailabilityRepository.save(availability);
        logger.info("Availability updated for slot {}", availabilityId);
        return availability;
    }
    
    /**
     * Delete room availability
     */
    public void deleteAvailability(Long availabilityId) {
        roomAvailabilityRepository.deleteById(availabilityId);
        logger.info("Availability slot deleted: {}", availabilityId);
    }
}
