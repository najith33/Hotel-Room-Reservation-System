package com.hotel.roomreservation.controller;

import com.hotel.roomreservation.model.*;
import com.hotel.roomreservation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/guest")
public class GuestController {
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private RoomService roomService;
    
    // @Autowired
    // private UserService userService; // Not currently used in this controller
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private com.hotel.roomreservation.service.AvailabilityService availabilityService;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        List<Booking> bookings = bookingService.getBookingsByGuest(user.getUserId());
        
        // Calculate booking counts
        long totalBookings = bookings != null ? bookings.size() : 0;
        long pendingBookings = bookings != null ? bookings.stream()
                .filter(b -> b != null && b.getBookingStatus() == Booking.BookingStatus.PENDING)
                .count() : 0;
        long confirmedBookings = bookings != null ? bookings.stream()
                .filter(b -> b != null && b.getBookingStatus() == Booking.BookingStatus.CONFIRMED)
                .count() : 0;
        long cancelledBookings = bookings != null ? bookings.stream()
                .filter(b -> b != null && b.getBookingStatus() == Booking.BookingStatus.CANCELLED)
                .count() : 0;
        
        model.addAttribute("user", user);
        model.addAttribute("bookings", bookings);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("pendingBookings", pendingBookings);
        model.addAttribute("confirmedBookings", confirmedBookings);
        model.addAttribute("cancelledBookings", cancelledBookings);
        return "guest/dashboard";
    }
    
    @GetMapping("/book-room")
    public String bookRoom(Model model) {
        List<Room> rooms = roomService.getAllActiveRooms();
        model.addAttribute("rooms", rooms);
        model.addAttribute("booking", new Booking());
        model.addAttribute("decorPrefs", new DecorPreferences());
        model.addAttribute("cateringPrefs", new CateringPreferences());
        return "guest/book-room";
    }
    
    @PostMapping("/book-room")
    public String submitBooking(@RequestParam(required = false) Long roomId,
                               @RequestParam(required = false) String stayType,
                               @RequestParam(required = false) String reservationDate,
                               @RequestParam(required = false) String startTime,
                               @RequestParam(required = false) String endTime,
                               @RequestParam(required = false) Integer guestCount,
                               @RequestParam(required = false) String specialRequests,
                               @RequestParam(required = false) String theme,
                               @RequestParam(required = false) String colorScheme,
                               @RequestParam(required = false) String flowerArrangements,
                               @RequestParam(required = false) String lightingPreferences,
                               @RequestParam(required = false) String additionalDecorRequests,
                               @RequestParam(required = false) String mealType,
                               @RequestParam(required = false) String cuisineType,
                               @RequestParam(required = false) String dietaryRestrictions,
                               @RequestParam(required = false) String specialDishes,
                               @RequestParam(required = false) String beveragePreferences,
                               @RequestParam(required = false) String servingStyle,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            validateRequiredBookingFields(roomId, stayType, reservationDate, startTime, endTime, guestCount);
            User user = getCurrentUser(authentication);
            Room room = roomService.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));
            
            // Create decor preferences
            DecorPreferences decorPrefs = null;
            if (hasText(theme) || hasText(colorScheme) || hasText(flowerArrangements) ||
                hasText(lightingPreferences) || hasText(additionalDecorRequests)) {
                decorPrefs = new DecorPreferences();
                decorPrefs.setTheme(theme);
                decorPrefs.setColorScheme(colorScheme);
                decorPrefs.setFlowerArrangements(flowerArrangements);
                decorPrefs.setLightingPreferences(lightingPreferences);
                decorPrefs.setAdditionalDecorRequests(additionalDecorRequests);
            }
            
            // Catering is optional. An empty <option value=""> must not be parsed as an enum.
            validateOptionalCatering(mealType, cuisineType, dietaryRestrictions, specialDishes,
                    beveragePreferences, servingStyle);
            CateringPreferences cateringPrefs = null;
            if (hasText(mealType)) {
                cateringPrefs = new CateringPreferences();
                cateringPrefs.setMealType(CateringPreferences.MealType.valueOf(mealType.trim()));
                cateringPrefs.setCuisineType(cuisineType);
                cateringPrefs.setDietaryRestrictions(dietaryRestrictions);
                cateringPrefs.setSpecialDishes(specialDishes);
                cateringPrefs.setBeveragePreferences(beveragePreferences);
                if (hasText(servingStyle)) {
                    cateringPrefs.setServingStyle(CateringPreferences.ServingStyle.valueOf(servingStyle.trim()));
                }
            }
            
            Booking booking = bookingService.createBooking(
                user, room, stayType, 
                LocalDate.parse(reservationDate), 
                LocalTime.parse(startTime), 
                LocalTime.parse(endTime),
                guestCount, specialRequests, decorPrefs, cateringPrefs
            );
            
            redirectAttributes.addFlashAttribute("success", "Booking created successfully! Reference: " + booking.getReferenceCode());
            return "redirect:/guest/booking-confirmation/" + booking.getBookingId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/guest/book-room";
        }
    }
    
    @GetMapping("/booking-confirmation/{bookingId}")
    public String bookingConfirmation(@PathVariable Long bookingId, Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        Booking booking = bookingService.getBookingsByGuest(user.getUserId()).stream()
                .filter(b -> b.getBookingId().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        model.addAttribute("booking", booking);
        return "guest/booking-confirmation";
    }
    
    @GetMapping("/my-bookings")
    public String myBookings(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        List<Booking> bookings = bookingService.getBookingsByGuest(user.getUserId());
        
        model.addAttribute("user", user);
        model.addAttribute("bookings", bookings);
        return "guest/my-bookings";
    }
    
    @PostMapping("/cancel-booking/{bookingId}")
    public String cancelBooking(@PathVariable Long bookingId, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            // Verify booking exists and belongs to user
            bookingService.getBookingsByGuest(user.getUserId()).stream()
                    .filter(b -> b.getBookingId().equals(bookingId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            bookingService.cancelBooking(bookingId);
            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/guest/my-bookings";
    }

    // Gracefully handle accidental GET on cancel URL by redirecting with a message
    @GetMapping("/cancel-booking/{bookingId}")
    public String cancelBookingGetNotAllowed(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Cancellation must be performed via POST from My Bookings page.");
        return "redirect:/guest/my-bookings";
    }
    
    @GetMapping("/edit-booking/{bookingId}")
    public String editBooking(@PathVariable Long bookingId, Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        Booking booking = bookingService.getBookingsByGuest(user.getUserId()).stream()
                .filter(b -> b.getBookingId().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        // Only allow editing of pending bookings
        if (booking.getBookingStatus() != Booking.BookingStatus.PENDING) {
            throw new RuntimeException("Only pending bookings can be edited");
        }
        
        List<Room> rooms = roomService.getAllActiveRooms();
        model.addAttribute("booking", booking);
        model.addAttribute("rooms", rooms);
        model.addAttribute("decorPrefs", booking.getDecorPreferences() != null ? booking.getDecorPreferences() : new DecorPreferences());
        model.addAttribute("cateringPrefs", booking.getCateringPreferences() != null ? booking.getCateringPreferences() : new CateringPreferences());
        return "guest/edit-booking";
    }
    
    @PostMapping("/update-booking/{bookingId}")
    public String updateBooking(@PathVariable Long bookingId,
                               @RequestParam(required = false) String stayType,
                               @RequestParam(required = false) Long roomId,
                               @RequestParam(required = false) String reservationDate,
                               @RequestParam(required = false) String startTime,
                               @RequestParam(required = false) String endTime,
                               @RequestParam(required = false) Integer guestCount,
                               @RequestParam(required = false) String specialRequests,
                               @RequestParam(required = false) String theme,
                               @RequestParam(required = false) String colorScheme,
                               @RequestParam(required = false) String flowerArrangements,
                               @RequestParam(required = false) String lightingPreferences,
                               @RequestParam(required = false) String additionalDecorRequests,
                               @RequestParam(required = false) String mealType,
                               @RequestParam(required = false) String cuisineType,
                               @RequestParam(required = false) String dietaryRestrictions,
                               @RequestParam(required = false) String specialDishes,
                               @RequestParam(required = false) String beveragePreferences,
                               @RequestParam(required = false) String servingStyle,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            Booking booking = bookingService.getBookingsByGuest(user.getUserId()).stream()
                    .filter(b -> b.getBookingId().equals(bookingId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            // Only allow editing of pending bookings
            if (booking.getBookingStatus() != Booking.BookingStatus.PENDING) {
                throw new RuntimeException("Only pending bookings can be edited");
            }
            
            validateRequiredBookingFields(roomId, stayType, reservationDate, startTime, endTime, guestCount);

            // Update booking details
            booking.setStayType(stayType);
            Room room = roomService.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));
            booking.setRoom(room);
            booking.setReservationDate(LocalDate.parse(reservationDate));
            booking.setStartTime(LocalTime.parse(startTime));
            booking.setEndTime(LocalTime.parse(endTime));
            booking.setGuestCount(guestCount);
            booking.setSpecialRequests(specialRequests);
            
            // Update decor preferences
            if (hasText(theme) || hasText(colorScheme) || hasText(flowerArrangements) ||
                hasText(lightingPreferences) || hasText(additionalDecorRequests)) {
                DecorPreferences decorPrefs = booking.getDecorPreferences();
                if (decorPrefs == null) {
                    decorPrefs = new DecorPreferences();
                    decorPrefs.setBooking(booking);
                    booking.setDecorPreferences(decorPrefs);
                }
                decorPrefs.setTheme(theme);
                decorPrefs.setColorScheme(colorScheme);
                decorPrefs.setFlowerArrangements(flowerArrangements);
                decorPrefs.setLightingPreferences(lightingPreferences);
                decorPrefs.setAdditionalDecorRequests(additionalDecorRequests);
            }
            
            // Update catering only if selected; every catering detail other than meal type is optional.
            validateOptionalCatering(mealType, cuisineType, dietaryRestrictions, specialDishes,
                    beveragePreferences, servingStyle);
            if (hasText(mealType)) {
                CateringPreferences cateringPrefs = booking.getCateringPreferences();
                if (cateringPrefs == null) {
                    cateringPrefs = new CateringPreferences();
                    cateringPrefs.setBooking(booking);
                    booking.setCateringPreferences(cateringPrefs);
                }
                cateringPrefs.setMealType(CateringPreferences.MealType.valueOf(mealType.trim()));
                cateringPrefs.setCuisineType(cuisineType);
                cateringPrefs.setDietaryRestrictions(dietaryRestrictions);
                cateringPrefs.setSpecialDishes(specialDishes);
                cateringPrefs.setBeveragePreferences(beveragePreferences);
                cateringPrefs.setServingStyle(hasText(servingStyle)
                        ? CateringPreferences.ServingStyle.valueOf(servingStyle.trim()) : null);
            }
            
            // Recalculate total cost
            BigDecimal totalCost = bookingService.calculateTotalCost(room, LocalTime.parse(startTime), LocalTime.parse(endTime));
            booking.setTotalCost(totalCost);
            
            bookingService.updateBooking(booking);
            redirectAttributes.addFlashAttribute("success", "Booking updated successfully!");
            return "redirect:/guest/my-bookings";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update booking: " + e.getMessage());
            return "redirect:/guest/edit-booking/" + bookingId;
        }
    }
    
    @GetMapping("/rooms")
    public String viewRooms(Model model) {
        List<Room> rooms = roomService.getAllActiveRooms();
        model.addAttribute("rooms", rooms);
        return "guest/rooms";
    }
    
    @GetMapping("/availability")
    public String viewAvailability(@RequestParam(required = false) Long roomId,
                                 @RequestParam(required = false) String month,
                                 Model model) {
        LocalDate targetMonth = month != null ? LocalDate.parse(month + "-01") : LocalDate.now();
        
        Map<String, Object> calendarData = availabilityService.getCalendarData(targetMonth, roomId);
        
        // Extract data from calendarData map and add as individual attributes
        model.addAttribute("calendarData", calendarData);
        model.addAttribute("selectedRoomId", roomId);
        model.addAttribute("targetMonth", targetMonth);
        model.addAttribute("startOfMonth", calendarData.get("startOfMonth"));
        model.addAttribute("endOfMonth", calendarData.get("endOfMonth"));
        model.addAttribute("groupedByDate", calendarData.get("groupedByDate"));
        model.addAttribute("availabilities", calendarData.get("availabilities"));
        
        // Create calendar days list for the template
        List<LocalDate> calendarDays = new ArrayList<>();
        LocalDate startOfMonth = (LocalDate) calendarData.get("startOfMonth");
        LocalDate endOfMonth = (LocalDate) calendarData.get("endOfMonth");
        
        // Add days from start to end of month
        LocalDate currentDay = startOfMonth;
        while (!currentDay.isAfter(endOfMonth)) {
            calendarDays.add(currentDay);
            currentDay = currentDay.plusDays(1);
        }
        
        model.addAttribute("calendarDays", calendarDays);
        
        // Get all rooms for filter dropdown
        List<Room> rooms = roomService.getAllActiveRooms();
        model.addAttribute("rooms", rooms);
        
        return "guest/availability";
    }
    
    @GetMapping("/notifications")
    public String notifications(@RequestParam(required = false, defaultValue = "ALL") String status,
                                Authentication authentication, Model model) {
        try {
            User user = getCurrentUser(authentication);
            if (user == null) {
                model.addAttribute("error", "User not found");
                model.addAttribute("notifications", java.util.Collections.emptyList());
                model.addAttribute("unreadCount", 0L);
                model.addAttribute("status", "ALL");
                return "guest/notifications";
            }
            
            System.out.println("Guest notifications - User ID: " + user.getUserId());
            
            List<com.hotel.roomreservation.model.Notification> notifications = new java.util.ArrayList<>();
            Long unreadCount = 0L;
            
            try {
                System.out.println("Guest notifications - Attempting to fetch notifications for user: " + user.getUserId());
                
                switch (status.toUpperCase()) {
                    case "UNREAD":
                        System.out.println("Guest notifications - Fetching UNREAD notifications");
                        notifications = notificationService.getUnreadNotificationsForUser(user.getUserId());
                        break;
                    case "READ":
                        System.out.println("Guest notifications - Fetching READ notifications");
                        notifications = notificationService.getReadNotificationsForUser(user.getUserId());
                        break;
                    default:
                        System.out.println("Guest notifications - Fetching ALL notifications");
                        notifications = notificationService.getNotificationsForUser(user.getUserId());
                }
                
                System.out.println("Guest notifications - Successfully fetched " + notifications.size() + " notifications");
                
                System.out.println("Guest notifications - Attempting to get unread count");
                unreadCount = notificationService.getUnreadNotificationCount(user.getUserId());
                System.out.println("Guest notifications - Unread count: " + unreadCount);
                
            } catch (Exception serviceException) {
                System.err.println("Guest notifications service error: " + serviceException.getMessage());
                serviceException.printStackTrace();
                notifications = new java.util.ArrayList<>();
                unreadCount = 0L;
            }
            
            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", unreadCount);
            model.addAttribute("status", status.toUpperCase());
        } catch (Exception e) {
            System.err.println("Guest notifications error: " + e.getMessage());
            e.printStackTrace();
            // Ensure the view still renders even if an error occurs
            model.addAttribute("notifications", java.util.Collections.emptyList());
            model.addAttribute("unreadCount", 0L);
            model.addAttribute("status", "ALL");
            model.addAttribute("error", "Unable to load notifications: " + e.getMessage());
        }
        return "guest/notifications";
    }
    
    @PostMapping("/notifications/{notificationId}/mark-read")
    public String markNotificationAsRead(@PathVariable Long notificationId, RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(notificationId);
            redirectAttributes.addFlashAttribute("success", "Notification marked as read!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/guest/notifications";
    }
    
    @PostMapping("/notifications/{notificationId}/delete")
    public String deleteNotification(@PathVariable Long notificationId, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            if (notificationService.deleteNotificationForUser(notificationId, user.getUserId())) {
                redirectAttributes.addFlashAttribute("success", "Notification deleted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Notification not found or access denied!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/guest/notifications";
    }
    
    @PostMapping("/notifications/mark-all-read")
    public String markAllNotificationsAsRead(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            notificationService.markAllAsReadForUser(user.getUserId());
            redirectAttributes.addFlashAttribute("success", "All notifications marked as read!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/guest/notifications";
    }
    
    @PostMapping("/notifications/delete-all")
    public String deleteAllNotifications(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            notificationService.deleteAllNotificationsForUser(user.getUserId());
            redirectAttributes.addFlashAttribute("success", "All notifications deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/guest/notifications";
    }
    
    @PostMapping("/notifications/bulk-delete")
    public String bulkDeleteNotifications(@RequestParam("notificationIds") List<Long> notificationIds, 
                                       Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            int deletedCount = notificationService.deleteMultipleNotificationsForUser(notificationIds, user.getUserId());
            redirectAttributes.addFlashAttribute("success", deletedCount + " notifications deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/guest/notifications";
    }
    
    
    private User getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return null;
            }
            return ((com.hotel.roomreservation.service.CustomUserDetailsService.CustomUserPrincipal) 
                    authentication.getPrincipal()).getUser();
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static void validateRequiredBookingFields(Long roomId, String stayType,
            String reservationDate, String startTime, String endTime, Integer guestCount) {
        if (roomId == null) {
            throw new IllegalArgumentException("Please select a room.");
        }
        if (!hasText(stayType)) {
            throw new IllegalArgumentException("Please enter a stay type.");
        }
        if (!hasText(reservationDate)) {
            throw new IllegalArgumentException("Please select a reservation date.");
        }
        if (!hasText(startTime) || !hasText(endTime)) {
            throw new IllegalArgumentException("Please select both start and end times.");
        }
        if (guestCount == null || guestCount < 1) {
            throw new IllegalArgumentException("Guest count must be at least 1.");
        }
        try {
            LocalDate date = LocalDate.parse(reservationDate);
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            if (!date.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Please choose a future reservation date.");
            }
            if (!end.isAfter(start) || java.time.Duration.between(start, end).toMinutes() < 60) {
                throw new IllegalArgumentException("End time must be at least 1 hour after start time.");
            }
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("Please enter a valid reservation date and time.");
        }
    }

    private static void validateOptionalCatering(String mealType, String cuisineType,
            String dietaryRestrictions, String specialDishes, String beveragePreferences,
            String servingStyle) {
        boolean detailsEntered = hasText(cuisineType) || hasText(dietaryRestrictions)
                || hasText(specialDishes) || hasText(beveragePreferences) || hasText(servingStyle);
        if (!hasText(mealType) && detailsEntered) {
            throw new IllegalArgumentException(
                    "Choose a Meal Type to add catering preferences, or leave all catering fields empty.");
        }
    }
}
