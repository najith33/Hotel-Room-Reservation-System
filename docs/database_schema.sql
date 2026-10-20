-- Hotel Room Reservation System Database Schema
-- For XAMPP MySQL Server

CREATE DATABASE IF NOT EXISTS hotel_room_db;
USE hotel_room_db;

-- 1. User Management and Role-Based Access Control (RBAC)

-- Roles table
CREATE TABLE roles (
    role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Users table
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- User-Role junction table
CREATE TABLE user_roles (
    user_role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_role (user_id, role_id)
);

-- 2. Room Management

-- Rooms table
CREATE TABLE rooms (
    room_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_name VARCHAR(100) NOT NULL,
    room_type ENUM('STANDARD', 'DELUXE', 'SUITE') NOT NULL,
    capacity INT NOT NULL,
    description TEXT,
    hourly_rate DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Room Availability table
CREATE TABLE room_availability (
    availability_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status ENUM('AVAILABLE', 'BOOKED', 'MAINTENANCE', 'BLOCKED') DEFAULT 'AVAILABLE',
    booking_id BIGINT NULL,
    notes TEXT NULL,
    maintenance_reason VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    UNIQUE KEY unique_room_time (room_id, date, start_time, end_time)
);

-- 3. Room Booking and Customization

-- Bookings table
CREATE TABLE bookings (
    booking_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guest_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    stay_type VARCHAR(100) NOT NULL,
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    guest_count INT NOT NULL,
    total_cost DECIMAL(10,2) NOT NULL,
    booking_status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED') DEFAULT 'PENDING',
    reference_code VARCHAR(50) NOT NULL UNIQUE,
    qr_code_path TEXT,
    special_requests TEXT,
    assigned_coordinator_id BIGINT NULL,
    coordinator_notes TEXT NULL,
    setup_status VARCHAR(50) NULL,
    catering_notes TEXT NULL,
    catering_status VARCHAR(50) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (guest_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE
);

-- Decor Preferences table
CREATE TABLE decor_preferences (
    decor_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    theme VARCHAR(100),
    color_scheme VARCHAR(100),
    flower_arrangements TEXT,
    lighting_preferences TEXT,
    additional_decor_requests TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

-- Catering Preferences table
CREATE TABLE catering_preferences (
    catering_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    meal_type ENUM('BREAKFAST', 'LUNCH', 'DINNER', 'SNACKS', 'COCKTAILS') NOT NULL,
    cuisine_type VARCHAR(100),
    dietary_restrictions TEXT,
    special_dishes TEXT,
    beverage_preferences TEXT,
    serving_style ENUM('BUFFET', 'PLATED', 'FAMILY_STYLE', 'COCKTAIL') DEFAULT 'BUFFET',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

-- 4. Notifications and Alerts

-- Notifications table
CREATE TABLE notifications (
    notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_user_id BIGINT NOT NULL,
    sender_type ENUM('SYSTEM', 'STAFF') DEFAULT 'SYSTEM',
    sender_user_id BIGINT NULL,
    message_content TEXT NOT NULL,
    alert_type ENUM('GUEST_ARRIVAL', 'BOOKING_CHANGE', 'COORDINATION_ALERT', 'PAYMENT_REMINDER', 'RESERVATION_REMINDER', 'BOOKING_CONFIRMATION', 'BOOKING_CANCELLATION', 'SETUP_COMPLETE', 'CATERING_CONFIRMED') NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    FOREIGN KEY (recipient_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- Promotions table
CREATE TABLE promotions (
    promotion_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    promotion_name VARCHAR(100) NOT NULL,
    promotion_code VARCHAR(20) NOT NULL UNIQUE,
    discount_type VARCHAR(50) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    min_booking_amount DECIMAL(10,2) NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    description TEXT NULL,
    max_uses INT NULL,
    current_uses INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 5. Insert Initial Data

-- Insert default roles
INSERT INTO roles (role_name, description) VALUES
('GUEST', 'External customers who book rooms'),
('GENERAL_MANAGER', 'Oversees all bookings and schedules, manages internal coordination'),
('RESERVATION_COORDINATOR', 'Coordinates room preparation and guest requests'),
('CATERING_TEAM_LEADER', 'Manages meal and room-service requests'),
('MARKETING_EXECUTIVE', 'Analyzes bookings, trends, and manages promotions'),
('RECEPTIONIST', 'Verifies bookings using QR/Ref code and handles guest arrival');

-- Insert sample rooms
INSERT INTO rooms (room_name, room_type, capacity, description, hourly_rate) VALUES
('Room 101', 'STANDARD', 2, 'Comfortable standard room with queen bed and essential amenities', 45.00),
('Room 102', 'STANDARD', 2, 'Standard twin room suitable for short stays', 45.00),
('Room 201', 'DELUXE', 3, 'Deluxe room with king bed, work desk, and city view', 70.00),
('Room 202', 'DELUXE', 3, 'Spacious deluxe room with upgraded amenities', 75.00),
('Suite 301', 'SUITE', 4, 'Premium suite with separate living area and minibar', 110.00),
('Suite 302', 'SUITE', 4, 'Executive suite with lounge area and panoramic view', 125.00);

-- Insert all users with their actual data
INSERT INTO users (user_id, username, email, password, first_name, last_name, phone, is_active, created_at, updated_at) VALUES
(1, 'admin', 'admin@hotel.com', '$2a$10$LBQcFN2ketp/H7shylEopeE83FscbKEzU3TeFU6vJpgg/ZXZIax7K', 'System', 'Administrator', '1234567890', 1, '2025-10-21 15:32:28', '2025-10-21 10:36:08'),
(2, 'sithika', 'sithii.walisundara@gmail.com', '$2a$10$LBQcFN2ketp/H7shylEopeE83FscbKEzU3TeFU6vJpgg/ZXZIax7K', 'Sithika', 'Wathsula', '0723180882', 1, '2025-10-21 10:04:20', '2025-10-21 10:04:20'),
(3, 'catering', 'catering@hotel.com', '$2a$10$i4sD2A1Nu1mnrAIn10yHIOJ3SScQUhRQVQzQlfE0qtrqXoL4p.JqO', 'Catering', 'Service', '0123456789', 1, '2025-10-22 01:27:44', '2025-10-22 01:27:44'),
(4, 'coordinator', 'coordinator@hotel.com', '$2a$10$SVlrpR/ezJh1Noxz6Sc7ROrMs3hNR7L2tCAj/OUG5gaYkewoSn5ka', 'Reservation', 'Coordinator', '1111111111', 1, '2025-10-22 01:29:36', '2025-10-22 01:29:36'),
(5, 'marketing', 'marketing@hotel.com', '$2a$10$uDrxvL2LG7mx76rLDxEMmuya/.el50ogjcoNABzL3ODjyT7VQw6QG', 'Marketing', 'Executive', '2222222222', 1, '2025-10-22 01:30:54', '2025-10-22 01:30:54'),
(6, 'reception', 'reception@hotel.com', '$2a$10$0i1dQ8fKeeYHHmBukQ0oJeGyq3FK1yO01.GUIBtQKRVjHwONyW5qS', 'Recieption', 'Staff', '3333333333', 1, '2025-10-22 01:32:14', '2025-10-22 01:32:14');

-- Assign roles to users
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 2), -- admin -> GENERAL_MANAGER role (role_id=2)
(2, 1), -- sithika -> GUEST role (role_id=1)
(3, 4), -- catering -> CATERING_TEAM_LEADER role (role_id=4)
(4, 3), -- coordinator -> RESERVATION_COORDINATOR role (role_id=3)
(5, 5), -- marketing -> MARKETING_EXECUTIVE role (role_id=5)
(6, 6); -- reception -> RECEPTIONIST role (role_id=6)

-- Create indexes for better performance
CREATE INDEX idx_bookings_guest_id ON bookings(guest_id);
CREATE INDEX idx_bookings_room_id ON bookings(room_id);
CREATE INDEX idx_bookings_reservation_date ON bookings(reservation_date);
CREATE INDEX idx_bookings_status ON bookings(booking_status);
CREATE INDEX idx_room_availability_date ON room_availability(date);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_user_id);
CREATE INDEX idx_notifications_unread ON notifications(recipient_user_id, is_read);
