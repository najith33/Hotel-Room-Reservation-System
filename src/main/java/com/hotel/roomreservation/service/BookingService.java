package com.hotel.roomreservation.service;

import com.hotel.roomreservation.model.*;
import com.hotel.roomreservation.repository.BookingRepository;
import com.hotel.roomreservation.repository.RoomAvailabilityRepository;
import com.hotel.roomreservation.repository.RoomRepository;
import com.hotel.roomreservation.strategy.BookingStatusStrategy;
import com.hotel.roomreservation.util.QRCodeGenerator;
import com.hotel.roomreservation.util.ReferenceCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class BookingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private RoomAvailabilityRepository roomAvailabilityRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private QRCodeGenerator qrCodeGenerator;
    
    @Autowired
    private ReferenceCodeGenerator referenceCodeGenerator;
    
    @Autowired
    private Map<String, BookingStatusStrategy> bookingStatusStrategies;
    
    /**
     * Create a new booking
     */
    public Booking createBooking(User guest, Room room, String stayType, 
                                LocalDate reservationDate, LocalTime startTime, LocalTime endTime,
                                Integer guestCount, String specialRequests,
                                DecorPreferences decorPrefs, CateringPreferences cateringPrefs) {
        
        // Check room availability
        if (!isRoomAvailable(room.getRoomId(), reservationDate, startTime, endTime)) {
            throw new RuntimeException("Room is not available for the selected time slot");
        }
        
        // Calculate total cost
        BigDecimal totalCost = calculateTotalCost(room, startTime, endTime);
        
        // Generate reference code
        String referenceCode = referenceCodeGenerator.generateReferenceCode();
        
        // Create booking
        Booking booking = new Booking(guest, room, stayType, reservationDate, startTime, endTime, guestCount, totalCost);
        booking.setReferenceCode(referenceCode);
        booking.setSpecialRequests(specialRequests);
        
        // Generate QR code
        String qrCodeBase64 = qrCodeGenerator.generateQRCodeBase64(referenceCode);
        if (qrCodeBase64 != null) {
            booking.setQrCodePath("data:image/png;base64," + qrCodeBase64);
        }
        
        // Save booking
        booking = bookingRepository.save(booking);
        
        // Set up preferences
        if (decorPrefs != null) {
            decorPrefs.setBooking(booking);
            booking.setDecorPreferences(decorPrefs);
        }
        
        if (cateringPrefs != null) {
            cateringPrefs.setBooking(booking);
            booking.setCateringPreferences(cateringPrefs);
        }
        
        // Block room availability
        blockRoomAvailability(room.getRoomId(), reservationDate, startTime, endTime, booking.getBookingId());
        
        // Process booking using strategy pattern
        processBookingStatus(booking);
        
        logger.info("Booking created successfully: {}", booking.getReferenceCode());
        return booking;
    }
    
    /**
     * Update booking status
     */
    public Booking updateBookingStatus(Long bookingId, Booking.BookingStatus newStatus) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new RuntimeException("Booking not found");
        }
        
        Booking booking = bookingOpt.get();
        booking.setBookingStatus(newStatus);
        booking = bookingRepository.save(booking);
        
        // Process booking using strategy pattern
        processBookingStatus(booking);
        
        logger.info("Booking status updated to {} for booking: {}", newStatus, booking.getReferenceCode());
        return booking;
    }
    
    /**
     * Cancel booking
     */
    public Booking cancelBooking(Long bookingId) {
        return updateBookingStatus(bookingId, Booking.BookingStatus.CANCELLED);
    }
    
    /**
     * Confirm booking
     */
    public Booking confirmBooking(Long bookingId) {
        return updateBookingStatus(bookingId, Booking.BookingStatus.CONFIRMED);
    }
    
    /**
     * Complete booking
     */
    public Booking completeBooking(Long bookingId) {
        return updateBookingStatus(bookingId, Booking.BookingStatus.COMPLETED);
    }
    
    /**
     * Find booking by reference code
     */
    public Optional<Booking> findByReferenceCode(String referenceCode) {
        return bookingRepository.findByReferenceCode(referenceCode);
    }
    
    /**
     * Find booking by reference code with eagerly loaded related entities
     */
    public Optional<Booking> findByReferenceCodeWithDetails(String referenceCode) {
        return bookingRepository.findByReferenceCodeWithDetails(referenceCode);
    }
    
    /**
     * Get bookings by guest
     */
    public List<Booking> getBookingsByGuest(Long guestId) {
        return bookingRepository.findByGuestUserId(guestId);
    }
    
    /**
     * Get bookings by room
     */
    public List<Booking> getBookingsByRoom(Long roomId) {
        return bookingRepository.findByRoomRoomId(roomId);
    }
    
    /**
     * Get bookings by status
     */
    public List<Booking> getBookingsByStatus(Booking.BookingStatus status) {
        return bookingRepository.findByBookingStatus(status);
    }
    
    /**
     * Get bookings by date range
     */
    public List<Booking> getBookingsByDateRange(LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findByReservationDateBetween(startDate, endDate);
    }
    
    /**
     * Get bookings by assigned staff member
     */
    public List<Booking> getBookingsByAssignedStaff(Long staffId) {
        return bookingRepository.findByAssignedCoordinatorId(staffId);
    }
    
    /**
     * Get bookings with catering requirements
     */
    public List<Booking> getBookingsWithCatering() {
        return bookingRepository.findByCateringPreferencesIsNotNull();
    }
    
    /**
     * Find booking by ID
     */
    public Optional<Booking> findById(Long bookingId) {
        return bookingRepository.findById(bookingId);
    }
    
    /**
     * Update booking
     */
    public Booking updateBooking(Booking booking) {
        return bookingRepository.save(booking);
    }
    
    /**
     * Check if room is available
     */
    public boolean isRoomAvailable(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<RoomAvailability> conflicts = roomAvailabilityRepository.findConflictingAvailability(roomId, date, startTime, endTime, RoomAvailability.AvailabilityStatus.AVAILABLE);
        return conflicts.isEmpty();
    }
    
    /**
     * Calculate total cost
     */
    public BigDecimal calculateTotalCost(Room room, LocalTime startTime, LocalTime endTime) {
        long hours = java.time.Duration.between(startTime, endTime).toHours();
        return room.getHourlyRate().multiply(BigDecimal.valueOf(hours));
    }
    
    /**
     * Block room availability
     */
    private void blockRoomAvailability(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime, Long bookingId) {
        // Find existing availability slot to update
        List<RoomAvailability> existingSlots = roomAvailabilityRepository.findConflictingAvailability(
            roomId, date, startTime, endTime, RoomAvailability.AvailabilityStatus.AVAILABLE);
        
        if (!existingSlots.isEmpty()) {
            // Update existing slot to BOOKED status
            RoomAvailability slot = existingSlots.get(0);
            slot.setStatus(RoomAvailability.AvailabilityStatus.BOOKED);
            slot.setBookingId(bookingId);
            roomAvailabilityRepository.save(slot);
        } else {
            // Create new BOOKED slot if no existing slot found
            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                RoomAvailability availability = new RoomAvailability(
                    roomOpt.get(), date, startTime, endTime, RoomAvailability.AvailabilityStatus.BOOKED);
                availability.setBookingId(bookingId);
                roomAvailabilityRepository.save(availability);
            }
        }
    }
    
    /**
     * Process booking using strategy pattern
     */
    private void processBookingStatus(Booking booking) {
        try {
            if (booking == null || booking.getBookingStatus() == null) {
                logger.warn("Booking or booking status is null, skipping strategy processing");
                return;
            }
            
            String strategyKey = booking.getBookingStatus().toString().toLowerCase() + "BookingStrategy";
            BookingStatusStrategy strategy = bookingStatusStrategies.get(strategyKey);
            
            if (strategy != null) {
                strategy.processBooking(booking);
            } else {
                logger.warn("No strategy found for booking status: {}", booking.getBookingStatus());
            }
        } catch (Exception e) {
            logger.error("Error processing booking status for booking: {}", 
                        booking != null ? booking.getReferenceCode() : "null", e);
        }
    }
}
