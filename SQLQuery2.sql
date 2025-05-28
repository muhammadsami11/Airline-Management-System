---ADD NEW FLIGHT
USE AirlineManagementSystem;
INSERT INTO flights (flight_id, airline_name, origin, destination, departure_time, arrival_time, seat_capacity, available_seats) 
VALUES 
(104, 'AirNova', 'Berlin', 'Madrid', '07:30:00', '11:45:00', 180, 180);

---VIEW ALL RESERVATIONS
SELECT 
    r.reservation_id, 
    u.name AS passenger_name, 
    f.flight_id, 
    f.airline_name,
    r.seat_number, 
    r.booking_date, 
    r.status
FROM reservations r
JOIN users u ON r.user_id = u.user_id
JOIN flights f ON r.flight_id = f.flight_id;

--BOOK A TICKET
-- Step 1: Check flight availability
SELECT available_seats 
FROM flights 
WHERE flight_id = 101;
-- Step 2: Insert reservation (if available)
INSERT INTO reservations (reservation_id, user_id, flight_id, seat_number, booking_date, status) 
VALUES(3, 1, 101, 15, CAST(GETDATE() AS DATE), 'confirmed');
-- Step 3: Update available seats
UPDATE flights 
SET available_seats = available_seats - 1 
WHERE flight_id = 101;

--CANCEL BOOKING
-- Step 1: Cancel reservation
UPDATE reservations 
SET status = 'cancelled' 
WHERE reservation_id = 1;
-- Step 2: Increase available seats
UPDATE flights 
SET available_seats = available_seats + 1 
WHERE flight_id =(SELECT flight_id FROM reservations WHERE reservation_id = 1);

--VIEW MY BOOKINGS
SELECT 
    r.reservation_id, 
    f.flight_id, 
    f.airline_name, 
    f.origin, 
    f.destination, 
    r.seat_number, 
    r.booking_date, 
    r.status
FROM reservations r
JOIN flights f ON r.flight_id = f.flight_id
WHERE r.user_id = 1;

--FIND FLIGHT BY ID
SELECT * FROM flights WHERE flight_id = 101;

--LIST ALL FLIGHTS
SELECT * FROM flights;