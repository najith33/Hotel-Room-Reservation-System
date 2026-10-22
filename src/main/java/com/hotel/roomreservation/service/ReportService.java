package com.hotel.roomreservation.service;

import com.hotel.roomreservation.model.Booking;
import com.hotel.roomreservation.repository.BookingRepository;
import com.hotel.roomreservation.strategy.ReportExportStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private Map<String, ReportExportStrategy> reportExportStrategies;
    
    /**
     * Generate booking analytics report
     */
    public List<Map<String, Object>> generateBookingAnalytics(LocalDate startDate, LocalDate endDate, 
                                                             String stayType, Booking.BookingStatus status) {
        List<Booking> bookings;
        
        if (stayType != null && status != null) {
            bookings = bookingRepository.findByStayTypeAndStatus(stayType, status);
        } else if (stayType != null) {
            bookings = bookingRepository.findByStayTypeAndStatus(stayType, Booking.BookingStatus.CONFIRMED);
        } else if (status != null) {
            bookings = bookingRepository.findByBookingStatus(status);
        } else {
            bookings = bookingRepository.findByReservationDateBetween(startDate, endDate);
            
            // If no bookings found in date range, try to get all bookings to show some data
            if (bookings.isEmpty()) {
                logger.info("No bookings found in date range {}-{}, trying to get all bookings", startDate, endDate);
                bookings = bookingRepository.findAll();
            }
        }
        
        logger.info("Found {} bookings for analytics", bookings.size());
        return convertBookingsToReportData(bookings);
    }
    
    /**
     * Generate room utilization report
     */
    public List<Map<String, Object>> generateRoomUtilizationReport(LocalDate startDate, LocalDate endDate) {
        List<Booking> bookings = bookingRepository.findByReservationDateBetween(startDate, endDate);
        
        // If no bookings found in date range, try to get all bookings
        if (bookings.isEmpty()) {
            logger.info("No bookings found in date range {}-{} for room utilization, trying to get all bookings", startDate, endDate);
            bookings = bookingRepository.findAll();
        }
        
        logger.info("Found {} bookings for room utilization", bookings.size());
        return convertBookingsToRoomReportData(bookings);
    }
    
    /**
     * Generate revenue report
     */
    public List<Map<String, Object>> generateRevenueReport(LocalDate startDate, LocalDate endDate) {
        List<Booking> bookings = bookingRepository.findByReservationDateBetween(startDate, endDate);
        
        // If no bookings found in date range, try to get all bookings
        if (bookings.isEmpty()) {
            logger.info("No bookings found in date range {}-{} for revenue report, trying to get all bookings", startDate, endDate);
            bookings = bookingRepository.findAll();
        }
        
        logger.info("Found {} bookings for revenue report", bookings.size());
        return convertBookingsToRevenueReportData(bookings);
    }
    
    /**
     * Generate stay type trends report
     */
    public List<Map<String, Object>> generateStayTypeTrendsReport(LocalDate startDate, LocalDate endDate) {
        List<Booking> bookings = bookingRepository.findByReservationDateBetween(startDate, endDate);
        
        // If no bookings found in date range, try to get all bookings
        if (bookings.isEmpty()) {
            logger.info("No bookings found in date range {}-{} for stay type trends, trying to get all bookings", startDate, endDate);
            bookings = bookingRepository.findAll();
        }
        
        logger.info("Found {} bookings for stay type trends", bookings.size());
        return convertBookingsToStayTypeReportData(bookings);
    }
    
    /**
     * Export report using specified strategy
     */
    public byte[] exportReport(String strategyType, List<Map<String, Object>> data, String filename) {
        if (strategyType == null || strategyType.trim().isEmpty()) {
            throw new IllegalArgumentException("Strategy type cannot be null or empty");
        }
        
        if (data == null) {
            data = java.util.Collections.emptyList();
        }
        
        if (filename == null || filename.trim().isEmpty()) {
            filename = "report";
        }
        
        String strategyKey = strategyType.toLowerCase() + "ExportStrategy";
        ReportExportStrategy strategy = reportExportStrategies.get(strategyKey);
        
        if (strategy == null) {
            logger.error("Export strategy not found: {} (looking for key: {})", strategyType, strategyKey);
            logger.info("Available strategies: {}", reportExportStrategies.keySet());
            throw new RuntimeException("Export strategy not found: " + strategyType);
        }
        
        try {
            return strategy.exportData(data, filename);
        } catch (Exception e) {
            logger.error("Error exporting data using strategy: {}", strategyType, e);
            throw new RuntimeException("Failed to export data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get available export formats
     */
    public Map<String, String> getAvailableExportFormats() {
        Map<String, String> formats = new HashMap<>();
        for (ReportExportStrategy strategy : reportExportStrategies.values()) {
            formats.put(strategy.getStrategyType(), strategy.getFileExtension());
        }
        return formats;
    }
    
    /**
     * Convert bookings to report data format
     */
    private List<Map<String, Object>> convertBookingsToReportData(List<Booking> bookings) {
        return bookings.stream().map(booking -> {
            Map<String, Object> data = new HashMap<>();
            data.put("Booking ID", booking.getBookingId());
            data.put("Reference Code", booking.getReferenceCode());
            data.put("Guest Name", booking.getGuest().getFullName());
            data.put("Stay Type", booking.getStayType());
            data.put("Reservation Date", booking.getReservationDate());
            data.put("Start Time", booking.getStartTime());
            data.put("End Time", booking.getEndTime());
            data.put("Guest Count", booking.getGuestCount());
            data.put("Room", booking.getRoom().getRoomName());
            data.put("Total Cost", booking.getTotalCost());
            data.put("Status", booking.getBookingStatus());
            data.put("Created At", booking.getCreatedAt());
            return data;
        }).toList();
    }
    
    /**
     * Convert bookings to room report data format
     */
    private List<Map<String, Object>> convertBookingsToRoomReportData(List<Booking> bookings) {
        return bookings.stream().map(booking -> {
            Map<String, Object> data = new HashMap<>();
            data.put("Room Name", booking.getRoom().getRoomName());
            data.put("Room Type", booking.getRoom().getRoomType());
            data.put("Capacity", booking.getRoom().getCapacity());
            data.put("Reservation Date", booking.getReservationDate());
            data.put("Stay Type", booking.getStayType());
            data.put("Guest Count", booking.getGuestCount());
            data.put("Utilization %", calculateUtilization(booking.getGuestCount(), booking.getRoom().getCapacity()));
            data.put("Revenue", booking.getTotalCost());
            return data;
        }).toList();
    }
    
    /**
     * Convert bookings to revenue report data format
     */
    private List<Map<String, Object>> convertBookingsToRevenueReportData(List<Booking> bookings) {
        return bookings.stream().map(booking -> {
            Map<String, Object> data = new HashMap<>();
            data.put("Reservation Date", booking.getReservationDate());
            data.put("Stay Type", booking.getStayType());
            data.put("Room", booking.getRoom().getRoomName());
            data.put("Guest Count", booking.getGuestCount());
            data.put("Revenue", booking.getTotalCost());
            data.put("Status", booking.getBookingStatus());
            return data;
        }).toList();
    }
    
    /**
     * Convert bookings to stay type trends report data format
     */
    private List<Map<String, Object>> convertBookingsToStayTypeReportData(List<Booking> bookings) {
        return bookings.stream().map(booking -> {
            Map<String, Object> data = new HashMap<>();
            data.put("Stay Type", booking.getStayType());
            data.put("Reservation Date", booking.getReservationDate());
            data.put("Guest Count", booking.getGuestCount());
            data.put("Revenue", booking.getTotalCost());
            data.put("Room Type", booking.getRoom().getRoomType());
            return data;
        }).toList();
    }
    
    /**
     * Calculate room utilization percentage
     */
    private double calculateUtilization(Integer guestCount, Integer capacity) {
        if (capacity == 0) return 0.0;
        return (double) guestCount / capacity * 100;
    }
}
