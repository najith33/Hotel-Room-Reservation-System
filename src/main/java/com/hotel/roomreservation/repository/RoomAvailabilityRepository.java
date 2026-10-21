package com.hotel.roomreservation.repository;

import com.hotel.roomreservation.model.RoomAvailability;
import com.hotel.roomreservation.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface RoomAvailabilityRepository extends JpaRepository<RoomAvailability, Long> {
    
    List<RoomAvailability> findByRoomRoomIdAndDateAndStatus(Long roomId, LocalDate date, RoomAvailability.AvailabilityStatus status);
    
    @Query("SELECT va FROM RoomAvailability va WHERE va.room.roomId = :roomId AND va.date = :date AND va.status = :status AND " +
           "((va.startTime <= :startTime AND va.endTime > :startTime) OR " +
           "(va.startTime < :endTime AND va.endTime >= :endTime) OR " +
           "(va.startTime >= :startTime AND va.endTime <= :endTime))")
    List<RoomAvailability> findConflictingAvailability(@Param("roomId") Long roomId, 
                                                        @Param("date") LocalDate date,
                                                        @Param("startTime") LocalTime startTime,
                                                        @Param("endTime") LocalTime endTime,
                                                        @Param("status") RoomAvailability.AvailabilityStatus status);
    
    @Query("SELECT va FROM RoomAvailability va WHERE va.room.roomId = :roomId AND va.date BETWEEN :startDate AND :endDate ORDER BY va.date, va.startTime")
    List<RoomAvailability> findByRoomAndDateRange(@Param("roomId") Long roomId, 
                                                    @Param("startDate") LocalDate startDate, 
                                                    @Param("endDate") LocalDate endDate);
    
    List<RoomAvailability> findByDateAndStatus(LocalDate date, RoomAvailability.AvailabilityStatus status);
    
    List<RoomAvailability> findByDate(LocalDate date);
    
    @Query("SELECT va FROM RoomAvailability va WHERE va.room.roomType = :roomType AND va.date = :date AND va.status = :status")
    List<RoomAvailability> findByRoomTypeAndDateAndStatus(@Param("roomType") Room.RoomType roomType, 
                                                           @Param("date") LocalDate date,
                                                           @Param("status") RoomAvailability.AvailabilityStatus status);
    
    @Query("SELECT va FROM RoomAvailability va WHERE va.room.roomId = :roomId AND va.date = :date AND " +
           "((va.startTime <= :startTime AND va.endTime > :startTime) OR " +
           "(va.startTime < :endTime AND va.endTime >= :endTime) OR " +
           "(va.startTime >= :startTime AND va.endTime <= :endTime))")
    List<RoomAvailability> findByRoomAndDateAndTimeRange(@Param("roomId") Long roomId, 
                                                          @Param("date") LocalDate date,
                                                          @Param("startTime") LocalTime startTime,
                                                          @Param("endTime") LocalTime endTime);
    
}
