package com.hotel.roomreservation.repository;

import com.hotel.roomreservation.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    List<Room> findByIsActiveTrue();
    
    List<Room> findByRoomTypeAndIsActiveTrue(Room.RoomType roomType);
    
    @Query("SELECT v FROM Room v WHERE v.capacity >= :minCapacity AND v.isActive = true")
    List<Room> findByCapacityGreaterThanEqualAndIsActiveTrue(@Param("minCapacity") Integer minCapacity);
    
    @Query("SELECT v FROM Room v WHERE v.roomName LIKE %:name% AND v.isActive = true")
    List<Room> findByRoomNameContainingAndIsActiveTrue(@Param("name") String name);
    
    @Query("SELECT v FROM Room v WHERE v.hourlyRate <= :maxRate AND v.isActive = true ORDER BY v.hourlyRate ASC")
    List<Room> findByHourlyRateLessThanEqualAndIsActiveTrueOrderByHourlyRateAsc(@Param("maxRate") Double maxRate);
}
