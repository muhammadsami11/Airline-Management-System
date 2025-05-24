-- Airport Table
CREATE DATABASE AirlineManagementSystem;
USE AirlineManagementSystem;
CREATE TABLE users (
    user_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) CHECK (role IN ('admin', 'passenger')),
    passport_number VARCHAR(50),
    nationality VARCHAR(50)
);
CREATE TABLE flights (
    flight_id INT PRIMARY KEY,
    airline_name VARCHAR(100) NOT NULL,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    seat_capacity INT NOT NULL,
    available_seats INT NOT NULL
);
CREATE TABLE reservations (
    reservation_id INT PRIMARY KEY,
    user_id INT,
    flight_id INT,
    seat_number INT,
    booking_date DATE DEFAULT CAST(GETDATE() AS DATE),
    status VARCHAR(20) CHECK (status IN ('confirmed', 'cancelled')),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (flight_id) REFERENCES flights(flight_id)
);
-- USERS Table
INSERT INTO users (user_id, name, email, password, role, passport_number, nationality) VALUES
(1, 'Alice Johnson', 'alice@example.com', 'alicepass', 'passenger', 'P1234567', 'USA'),
(2, 'Bob Smith', 'bob@example.com', 'bobpass', 'admin', NULL, NULL),
(3, 'Charlie Adams', 'charlie@example.com', 'charliepass', 'passenger', 'P7654321', 'Canada');

-- FLIGHTS Table
INSERT INTO flights (flight_id, airline_name, origin, destination, departure_time, arrival_time, seat_capacity, available_seats) VALUES
(101, 'SkyJet', 'New York', 'London', '08:00:00', '20:00:00', 150, 150),
(102, 'AirExpress', 'Paris', 'Dubai', '10:00:00', '18:00:00', 180, 180),
(103, 'GlobalFly', 'Delhi', 'Tokyo', '06:00:00', '14:00:00', 200, 200);

-- RESERVATIONS Table
INSERT INTO reservations (reservation_id, user_id, flight_id, seat_number, booking_date, status) VALUES
(1, 1, 101, 12, CAST(GETDATE() AS DATE), 'confirmed'),
(2, 3, 103, 24, CAST(GETDATE() AS DATE), 'confirmed');
