package com.hotel.roomreservation.repository;

import com.hotel.roomreservation.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByGuestUserId(Long guestId);
    
    List<Booking> findByRoomRoomId(Long roomId);
    
    List<Booking> findByBookingStatus(Booking.BookingStatus status);
    
    Optional<Booking> findByReferenceCode(String referenceCode);
    
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.room WHERE b.referenceCode = :referenceCode")
    Optional<Booking> findByReferenceCodeWithDetails(@Param("referenceCode") String referenceCode);
    
    @Query("SELECT b FROM Booking b WHERE b.reservationDate = :date AND b.bookingStatus IN ('PENDING', 'CONFIRMED')")
    List<Booking> findByReservationDateAndActiveStatus(@Param("date") LocalDate date);
    
    @Query("SELECT b FROM Booking b WHERE b.reservationDate BETWEEN :startDate AND :endDate ORDER BY b.reservationDate, b.startTime")
    List<Booking> findByReservationDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.room.roomId = :roomId AND b.reservationDate = :date AND b.bookingStatus IN ('PENDING', 'CONFIRMED')")
    List<Booking> findByRoomAndDateAndActiveStatus(@Param("roomId") Long roomId, @Param("date") LocalDate date);
    
    @Query("SELECT b FROM Booking b WHERE b.guest.email = :email ORDER BY b.createdAt DESC")
    List<Booking> findByGuestEmailOrderByCreatedAtDesc(@Param("email") String email);
    
    @Query("SELECT b FROM Booking b WHERE b.stayType = :stayType AND b.bookingStatus = :status")
    List<Booking> findByStayTypeAndStatus(@Param("stayType") String stayType, @Param("status") Booking.BookingStatus status);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.reservationDate = :date AND b.bookingStatus IN ('PENDING', 'CONFIRMED')")
    Long countActiveBookingsByDate(@Param("date") LocalDate date);
    
    @Query("SELECT b FROM Booking b WHERE b.bookingStatus = 'PENDING' AND b.createdAt < :deadline")
    List<Booking> findPendingBookingsBeforeDeadline(@Param("deadline") java.time.LocalDateTime deadline);
    
    List<Booking> findByAssignedCoordinatorId(Long coordinatorId);
    
    List<Booking> findByCateringPreferencesIsNotNull();
}
