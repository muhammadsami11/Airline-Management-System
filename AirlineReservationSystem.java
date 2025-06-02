package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class Database {
    private Connection getConnection() throws SQLException {
        String url = "jdbc:sqlserver://DESKTOP-LIT6NNT\\SQLEXPRESS:1433;databaseName=AirlineManagementSystem;encrypt=true;trustServerCertificate=true";
        String user = "DB";
        String password = "sami1";
        return DriverManager.getConnection(url, user, password);
    }

    public boolean selectAdmin(String userID, String password) {
        String query = "SELECT 1 FROM users WHERE user_id = ? AND password = ? AND role = 'admin'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, Integer.parseInt(userID));
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Error in admin login: " + e.getMessage());
            return false;
        }
    }

    public boolean selectPassenger(String userID, String password, String passport) {
        String query = "SELECT 1 FROM users WHERE user_id = ? AND password = ? AND passport_number = ? AND role = 'passenger'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, Integer.parseInt(userID));
            stmt.setString(2, password);
            stmt.setString(3, passport);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Error in passenger login: " + e.getMessage());
            return false;
        }
    }

    public boolean insertFlight(Flight flight) {
        String query = "INSERT INTO flights (flight_id, airline_name, origin, destination, departure_time, arrival_time, seat_capacity, available_seats) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, Integer.parseInt(flight.getFlightID()));
            stmt.setString(2, flight.getAirlineName());
            stmt.setString(3, flight.getOrigin());
            stmt.setString(4, flight.getDestination());
            stmt.setString(5, flight.getDepartureTime());
            stmt.setString(6, flight.getArrivalTime());
            stmt.setInt(7, flight.getSeatCapacity());
            stmt.setInt(8, flight.getAvailableSeats());
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Error inserting flight: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<Reservation> getAllReservations() {
        ArrayList<Reservation> reservations = new ArrayList<>();
        String query = "SELECT r.reservation_id, r.user_id, r.flight_id, r.seat_number, "
                + "r.booking_date, r.status, u.name AS passenger_name, u.passport_number, "
                + "f.airline_name, f.origin, f.destination, f.departure_time, f.arrival_time "
                + "FROM reservations r "
                + "JOIN users u ON r.user_id = u.user_id "
                + "JOIN flights f ON r.flight_id = f.flight_id";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Reservation reservation = new Reservation();
                Passenger passenger = new Passenger();
                Flight flight = new Flight();

                reservation.reservationID = "R" + rs.getInt("reservation_id");
                reservation.seatNumber = String.valueOf(rs.getInt("seat_number"));
                reservation.bookingDate = rs.getDate("booking_date").toString();
                reservation.status = rs.getString("status");

                passenger.userID = String.valueOf(rs.getInt("user_id"));
                passenger.name = rs.getString("passenger_name");
                passenger.passportNumber = rs.getString("passport_number");

                flight.setFlightID(String.valueOf(rs.getInt("flight_id")));
                flight.setAirlineName(rs.getString("airline_name"));
                flight.setOrigin(rs.getString("origin"));
                flight.setDestination(rs.getString("destination"));
                flight.setDepartureTime(rs.getString("departure_time"));
                flight.setArrivalTime(rs.getString("arrival_time"));

                reservation.passenger = passenger;
                reservation.flight = flight;

                reservations.add(reservation);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving reservations: " + e.getMessage());
        }
        return reservations;
    }

    public boolean createReservation(Reservation reservation) {
        String query = "INSERT INTO reservations (reservation_id, user_id, flight_id, seat_number, booking_date, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            int resId = Integer.parseInt(reservation.reservationID.substring(1));
            int userId = Integer.parseInt(reservation.passenger.userID);
            int flightId = Integer.parseInt(reservation.flight.getFlightID());
            int seatNum = Integer.parseInt(reservation.seatNumber);

            stmt.setInt(1, resId);
            stmt.setInt(2, userId);
            stmt.setInt(3, flightId);
            stmt.setInt(4, seatNum);
            stmt.setDate(5, Date.valueOf(reservation.bookingDate));
            stmt.setString(6, reservation.status);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                return updateAvailableSeats(flightId, -1);
            }
            return false;
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("Error creating reservation: " + e.getMessage());
            return false;
        }
    }

    private boolean updateAvailableSeats(int flightId, int change) {
        String query = "UPDATE flights SET available_seats = available_seats + ? WHERE flight_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, change);
            stmt.setInt(2, flightId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating seats: " + e.getMessage());
            return false;
        }
    }

    public boolean isSeatAvailable(int flightId, int seatNumber) {
        String query = "SELECT 1 FROM reservations WHERE flight_id = ? AND seat_number = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, flightId);
            stmt.setInt(2, seatNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                return !rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking seat availability: " + e.getMessage());
            return false;
        }
    }

    public int getNextReservationId() {
        String query = "SELECT MAX(reservation_id) FROM reservations";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            return rs.next() ? rs.getInt(1) + 1 : 1;
        } catch (SQLException e) {
            System.err.println("Error getting next reservation ID: " + e.getMessage());
            return 1;
        }
    }

    public ArrayList<Flight> getAllFlights() {
        ArrayList<Flight> flights = new ArrayList<>();
        String query = "SELECT * FROM flights";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Flight flight = new Flight();
                flight.setFlightID(String.valueOf(rs.getInt("flight_id")));
                flight.setAirlineName(rs.getString("airline_name"));
                flight.setOrigin(rs.getString("origin"));
                flight.setDestination(rs.getString("destination"));
                flight.setDepartureTime(rs.getString("departure_time"));
                flight.setArrivalTime(rs.getString("arrival_time"));
                flight.setSeatCapacity(rs.getInt("seat_capacity"));
                flight.setAvailableSeats(rs.getInt("available_seats"));
                flights.add(flight);
            }
        } catch (SQLException e) {
            System.err.println("Error loading flights: " + e.getMessage());
        }
        return flights;
    }
}

abstract class User {
    protected String userID, name, email, password;

    public String getUserID() {
        return userID;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public abstract boolean login();

    public void logout() {
        System.out.println(name + " logged out.");
    }

    public void viewProfile() {
        System.out.println("Name: " + name + ", Email: " + email);
    }
}

class Admin extends User {
    @Override
    public boolean login() {
        Database d1 = new Database();
        return d1.selectAdmin(userID, password);
    }

    public void addFlight(ArrayList<Flight> flights, Scanner sc) {
        try {
            Flight flight = new Flight();
            flight.setFlightID(AirlineReservationSystem.getNonEmptyInput(sc, "Enter Flight ID: "));
            flight.setAirlineName(AirlineReservationSystem.getNonEmptyInput(sc, "Enter Airline Name: "));
            flight.setOrigin(AirlineReservationSystem.getValidCityName(sc, "Enter Origin: "));
            flight.setDestination(AirlineReservationSystem.getValidCityName(sc, "Enter Destination: "));
            flight.setDepartureTime(AirlineReservationSystem.getValidTime(sc, "Enter Departure Time (HH:MM): "));
            flight.setArrivalTime(AirlineReservationSystem.getValidTime(sc, "Enter Arrival Time (HH:MM): "));

            while (true) {
                try {
                    System.out.print("Enter Seat Capacity: ");
                    flight.setSeatCapacity(Integer.parseInt(sc.nextLine()));
                    if (flight.getSeatCapacity() > 0) {
                        break;
                    } else {
                        System.out.println("Seat capacity must be positive.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number. Try again.");
                }
            }

            flight.setAvailableSeats(flight.getSeatCapacity());
            Database db = new Database();
            boolean dbSuccess = db.insertFlight(flight);
            if (dbSuccess) {
                flights.add(flight);
                System.out.println("Flight added successfully to both database and system.");
            } else {
                System.out.println("Flight added to database failed. Not added to system.");
            }

        } catch (Exception e) {
            System.out.println("Error adding flight: " + e.getMessage());
        }
    }

    public void viewAllReservations() {
        Database d1 = new Database();
        ArrayList<Reservation> reservations = d1.getAllReservations();
        if (reservations.isEmpty()) {
            System.out.println("No reservations found");
        }
        for (Reservation r : reservations) {
            System.out.printf("reservationID: %s, passenger: %s, FlightID: %s, AirlineName: %s, status: %s%n",
                    r.reservationID, r.passenger.name, r.flight.getFlightID(), r.flight.getAirlineName(), r.status);
        }
    }
}

class Passenger extends User {
    protected String passportNumber, nationality;

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    @Override
    public boolean login() {
        Database d1 = new Database();
        return d1.selectPassenger(userID, password, passportNumber);
    }

    public void bookTicket(Flight flight, ArrayList<Reservation> reservations, Scanner sc) {
        try {
            if (!flight.checkAvailability()) {
                System.out.println("No available seats on this flight.");
                return;
            }

            int seatNumber = 0;
            while (true) {
                try {
                    String seatInput = AirlineReservationSystem.getNonEmptyInput(sc, "Enter seat number: ");
                    seatNumber = Integer.parseInt(seatInput);

                    Database db = new Database();
                    if (db.isSeatAvailable(
                            Integer.parseInt(flight.getFlightID()),
                            seatNumber
                    )) {
                        break;
                    } else {
                        System.out.println("Seat " + seatNumber + " is already taken.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid seat number. Please enter a number.");
                }
            }

            Reservation reservation = new Reservation();
            Database db = new Database();
            reservation.reservationID = "R" + db.getNextReservationId();
            reservation.passenger = this;
            reservation.flight = flight;
            reservation.seatNumber = String.valueOf(seatNumber);
            reservation.bookingDate = java.time.LocalDate.now().toString();
            reservation.status = "confirmed";

            if (db.createReservation(reservation)) {
                reservations.add(reservation);
                flight.updateSeats(-1);
                System.out.println(" Ticket booked successfully!");
                System.out.println("Reservation ID: " + reservation.reservationID);
                reservation.generateReceipt();
            } else {
                System.out.println("Failed to book ticket. Please try again.");
            }
        } catch (Exception e) {
            System.out.println("Error booking ticket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void viewMyBookings() {
        Database db = new Database();
        ArrayList<Reservation> allReservations = db.getAllReservations();
        int userId = Integer.parseInt(this.userID);
        boolean found = false;

        System.out.println("\n=== YOUR BOOKINGS ===");
        for (Reservation r : allReservations) {
            if (Integer.parseInt(r.passenger.userID) == userId) {
                r.generateReceipt();
                found = true;
            }
        }

        if (!found) {
            System.out.println("No bookings found for this passenger.");
        }
    }
}

class Flight {
    private String flightID, airlineName, origin, destination;
    private String departureTime, arrivalTime;
    private int seatCapacity, availableSeats;

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public void setFlightID(String flightID) {
        this.flightID = flightID;
    }

    public String getFlightID() {
        return flightID;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAirlineName(String airlineName) {
        this.airlineName = airlineName;
    }

    public String getAirlineName() {
        return airlineName;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOrigin() {
        return origin;
    }

    public int getSeatCapacity() {
        return seatCapacity;
    }

    public void setSeatCapacity(int seatCapacity) {
        this.seatCapacity = seatCapacity;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

    public boolean checkAvailability() {
        return availableSeats > 0;
    }

    public void updateSeats(int change) {
        availableSeats += change;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flight flight = (Flight) o;
        return flightID.equals(flight.flightID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightID);
    }

    @Override
    public String toString() {
        return String.format("Flight ID: %s, Airline: %s, %s -> %s, Dep: %s, Arr: %s, Seats: %d/%d",
                flightID, airlineName, origin, destination, departureTime, arrivalTime, availableSeats, seatCapacity);
    }
}

class Reservation {
    String reservationID;
    Passenger passenger;
    Flight flight;
    String seatNumber, bookingDate;
    String status;

    public void generateReceipt() {
        System.out.println("Receipt - ID: " + reservationID + ", Flight: " + flight.getFlightID() + ", Status: " + status);
    }

    @Override
    public String toString() {
        return String.format("Res ID: %s, Passenger: %s (ID: %s, Passport: %s), Flight: %s (%s -> %s), Seat: %s, Date: %s, Status: %s",
                reservationID, passenger.getName(), passenger.getUserID(), passenger.getPassportNumber(),
                flight.getFlightID(), flight.getOrigin(), flight.getDestination(), seatNumber, bookingDate, status);
    }
}

class AirlineSystem {
    ArrayList<Flight> flights = new ArrayList<>();
    ArrayList<Reservation> reservations = new ArrayList<>();

    public Flight findFlightById(String id) {
        for (Flight f : flights) {
            if (f.getFlightID().equalsIgnoreCase(id)) return f;
        }
        return null;
    }

    public void initializeSystem() {
        Database db = new Database();
        this.flights = db.getAllFlights();
        this.reservations = db.getAllReservations();
        System.out.println("System initialized. Loaded " + flights.size() + " flights and " + reservations.size() + " reservations.");
    }
}

public class AirlineReservationSystem {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
            SwingUtilities.invokeLater(() -> new AirlineGUI().start());
        } else {
            Scanner sc = new Scanner(System.in);
            AirlineSystem system = new AirlineSystem();
            system.initializeSystem();

            while (true) {
                System.out.println("\n--- Airline Reservation Menu ---");
                System.out.println("1. Admin Login");
                System.out.println("2. Passenger Login");
                System.out.println("3. Exit");

                int choice = getChoiceInRange(sc, "Select option: ", 1, 3);

                if (choice == 1) {
                    Admin admin = new Admin();
                    boolean loggedIn = false;
                    while (!loggedIn) {
                        admin.setUserID(getValidUserID(sc, "Enter Admin ID (digits only): "));
                        admin.setPassword(getValidPassword(sc, "Enter Password: "));
                        loggedIn = admin.login();
                        if (!loggedIn) {
                            System.out.println("Invalid admin credentials. Please try again.");
                        }
                    }

                    while (true) {
                        System.out.println("\nAdmin Menu:");
                        System.out.println("1. Add Flight");
                        System.out.println("2. View All Reservations");
                        System.out.println("3. Logout");

                        int adminChoice = getChoiceInRange(sc, "Choice: ", 1, 3);

                        if (adminChoice == 1) {
                            admin.addFlight(system.flights, sc);
                        } else if (adminChoice == 2) {
                            admin.viewAllReservations();
                        } else {
                            admin.logout();
                            break;
                        }
                    }

                } else if (choice == 2) {
                    Passenger passenger = new Passenger();
                    boolean loggedIn = false;

                    while (!loggedIn) {
                        passenger.setUserID(getValidUserID(sc, "Enter Passenger ID (digits only): "));
                        passenger.setPassword(getValidPassword(sc, "Enter Password: "));
                        passenger.setPassportNumber(getNonEmptyInput(sc, "Enter Passport Number: "));
                        loggedIn = passenger.login();
                        if (!loggedIn) {
                            System.out.println("Invalid passenger credentials. Please try again.");
                        }
                    }

                    while (true) {
                        System.out.println("\nPassenger Menu:");
                        System.out.println("1. Book Ticket");
                        System.out.println("2. View My Bookings");
                        System.out.println("3. Logout");

                        int passChoice = getChoiceInRange(sc, "Choice: ", 1, 3);

                        if (passChoice == 1) {
                            System.out.println("\nAvailable Flights:");
                            for (Flight f : system.flights) {
                                if (f.checkAvailability()) {
                                    System.out.printf("%s: %s → %s (%d seats available)\n",
                                            f.getFlightID(), f.getOrigin(), f.getDestination(),
                                            f.getAvailableSeats());
                                }
                            }

                            String fid = AirlineReservationSystem.getNonEmptyInput(sc, "Enter Flight ID to book: ");
                            Flight flight = system.findFlightById(fid);

                            if (flight != null) {
                                if (flight.checkAvailability()) {
                                    passenger.bookTicket(flight, system.reservations, sc);
                                } else {
                                    System.out.println("No available seats on this flight.");
                                }
                            } else {
                                System.out.println("Flight not found. Valid IDs: " +
                                        system.flights.stream().map(Flight::getFlightID).collect(Collectors.joining(", ")));
                            }

                        } else if (passChoice == 2) {
                            passenger.viewMyBookings();
                        } else {
                            passenger.logout();
                            break;
                        }
                    }

                } else if (choice == 3) {
                    System.out.println("Exiting system. Goodbye!");
                    break;
                }
            }
            sc.close();
        }
    }

    public static int getIntInput(Scanner sc) {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Try again.");
            }
        }
    }

    public static String getNonEmptyInput(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("This field cannot be empty. Please try again.");
        }
    }

    public static String getValidEmail(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String email = sc.nextLine().trim();
            if (email.contains("@") && email.indexOf('@') > 0 && email.indexOf('@') < email.length() - 1) {
                return email;
            }
            System.out.println("Invalid email format. It must contain '@'.");
        }
    }

    public static String getValidPassword(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String password = sc.nextLine().trim();
            if (password.length() >= 4) {
                return password;
            }
            System.out.println("Password must be at least 4 characters long.");
        }
    }

    public static String getValidName(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String name = sc.nextLine().trim();
            if (name.matches("^[a-zA-Z ]+$")) {
                return name;
            }
            System.out.println("Invalid name. Only letters and spaces are allowed.");
        }
    }

    public static String getValidUserID(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String id = sc.nextLine().trim();
            if (id.matches("^\\d+$")) {
                return id;
            }
            System.out.println("Invalid ID. Only digits are allowed.");
        }
    }

    public static int getChoiceInRange(Scanner sc, String prompt, int min, int max) {
        while (true) {
            try {
                System.out.print(prompt);
                int choice = Integer.parseInt(sc.nextLine());
                if (choice >= min && choice <= max) {
                    return choice;
                }
                System.out.println("Choice must be between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    public static String getValidCityName(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.matches("^[a-zA-Z ]+$")) {
                return input;
            }
            System.out.println("Invalid input. Only letters and spaces are allowed.");
        }
    }

    public static String getValidTime(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
                return input;
            }
            System.out.println("Invalid time format. Use HH:MM (24-hour).");
        }
    }
}

class AirlineGUI {
    private ArrayList<Flight> flights = new ArrayList<>();
    private ArrayList<Reservation> reservations = new ArrayList<>();
    private Database db = new Database();
    private Passenger currentPassenger = null;

    public void start() {
        loadData();
        createAndShowGUI();
    }

    private void loadData() {
        flights = db.getAllFlights();
        reservations = db.getAllReservations();
        System.out.println("Loaded " + flights.size() + " flights and " + reservations.size() + " reservations");
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Airline Reservation System");
        frame.setSize(450, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 30, 50));

        JButton adminBtn = new JButton("Admin Login");
        JButton passengerBtn = new JButton("Passenger Login");

        adminBtn.addActionListener(e -> showAdminLogin(frame));
        passengerBtn.addActionListener(e -> showPassengerLogin(frame));

        mainPanel.add(adminBtn);
        mainPanel.add(passengerBtn);

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void showAdminLogin(JFrame parent) {
        JTextField userIdField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);

        JPanel loginPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        loginPanel.add(new JLabel("Admin ID:"));
        loginPanel.add(userIdField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);

        int option = JOptionPane.showConfirmDialog(parent, loginPanel, "Admin Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String userId = userIdField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (!userId.matches("^\\d+$")) {
                JOptionPane.showMessageDialog(parent, "Admin ID must be digits only.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Password cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (db.selectAdmin(userId, password)) {
                JOptionPane.showMessageDialog(parent, "Admin login successful!", "Login Success", JOptionPane.INFORMATION_MESSAGE);
                showAdminMenu(parent);
            } else {
                JOptionPane.showMessageDialog(parent, "Invalid admin credentials", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAdminMenu(JFrame parent) {
        Admin admin = new Admin();
        admin.setUserID("2");

        String[] options = {"Add Flight", "View All Reservations", "Logout"};
        while (true) {
            int choice = JOptionPane.showOptionDialog(parent, "Admin Menu", "Admin Panel",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                addFlightGUI(parent);
            } else if (choice == 1) {
                viewAllReservationsGUI(parent);
            } else {
                admin.logout();
                JOptionPane.showMessageDialog(parent, "Admin logged out.", "Logout", JOptionPane.INFORMATION_MESSAGE);
                break;
            }
        }
    }

    private void addFlightGUI(JFrame parent) {
        JTextField idField = new JTextField(10);
        JTextField airlineField = new JTextField(10);
        JTextField originField = new JTextField(10);
        JTextField destField = new JTextField(10);
        JTextField depTimeField = new JTextField(10);
        JTextField arrTimeField = new JTextField(10);
        JTextField capacityField = new JTextField(10);

        JPanel panel = new JPanel(new GridLayout(7, 2, 5, 5));
        panel.add(new JLabel("Flight ID:")); panel.add(idField);
        panel.add(new JLabel("Airline Name:")); panel.add(airlineField);
        panel.add(new JLabel("Origin:")); panel.add(originField);
        panel.add(new JLabel("Destination:")); panel.add(destField);
        panel.add(new JLabel("Departure Time (HH:MM):")); panel.add(depTimeField);
        panel.add(new JLabel("Arrival Time (HH:MM):")); panel.add(arrTimeField);
        panel.add(new JLabel("Seat Capacity:")); panel.add(capacityField);

        int option = JOptionPane.showConfirmDialog(parent, panel, "Add Flight", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String flightID = idField.getText().trim();
                String airlineName = airlineField.getText().trim();
                String origin = originField.getText().trim();
                String destination = destField.getText().trim();
                String departureTime = depTimeField.getText().trim();
                String arrivalTime = arrTimeField.getText().trim();
                String capacityStr = capacityField.getText().trim();

                if (flightID.isEmpty() || airlineName.isEmpty() || origin.isEmpty() || destination.isEmpty() ||
                        departureTime.isEmpty() || arrivalTime.isEmpty() || capacityStr.isEmpty()) {
                    JOptionPane.showMessageDialog(parent, "All fields must be filled.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!flightID.matches("^\\d+$")) {
                    JOptionPane.showMessageDialog(parent, "Flight ID must be digits only.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!isValidCityName(origin) || !isValidCityName(destination)) {
                    JOptionPane.showMessageDialog(parent, "Origin and Destination must contain only letters and spaces.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!isValidTime(departureTime) || !isValidTime(arrivalTime)) {
                    JOptionPane.showMessageDialog(parent, "Time must be in HH:MM (24-hour) format.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int seatCapacity = Integer.parseInt(capacityStr);
                if (seatCapacity <= 0) {
                    JOptionPane.showMessageDialog(parent, "Seat capacity must be positive.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Flight flight = new Flight();
                flight.setFlightID(flightID);
                flight.setAirlineName(airlineName);
                flight.setOrigin(origin);
                flight.setDestination(destination);
                flight.setDepartureTime(departureTime);
                flight.setArrivalTime(arrivalTime);
                flight.setSeatCapacity(seatCapacity);
                flight.setAvailableSeats(seatCapacity);

                if (db.insertFlight(flight)) {
                    flights.add(flight);
                    JOptionPane.showMessageDialog(parent, "Flight added successfully!");
                } else {
                    JOptionPane.showMessageDialog(parent, "Failed to add flight. Check Flight ID uniqueness or database connection.", "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, "Invalid number format for Seat Capacity.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "An unexpected error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void viewAllReservationsGUI(JFrame parent) {
        loadData();
        ArrayList<Reservation> currentReservations = db.getAllReservations();

        if (currentReservations.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No reservations found.", "View Reservations", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] columnNames = {"Res ID", "Passenger Name", "Flight ID", "Origin", "Destination", "Seat No.", "Booking Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        for (Reservation r : currentReservations) {
            Object[] rowData = {
                    r.reservationID,
                    r.passenger.getName(),
                    r.flight.getFlightID(),
                    r.flight.getOrigin(),
                    r.flight.getDestination(),
                    r.seatNumber,
                    r.bookingDate,
                    r.status
            };
            model.addRow(rowData);
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 400));

        JOptionPane.showMessageDialog(parent, scrollPane, "All Reservations", JOptionPane.PLAIN_MESSAGE);
    }

    private void showPassengerLogin(JFrame parent) {
        JTextField userIdField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JTextField passportField = new JTextField(15);

        JPanel loginPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        loginPanel.add(new JLabel("Passenger ID:"));
        loginPanel.add(userIdField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);
        loginPanel.add(new JLabel("Passport Number:"));
        loginPanel.add(passportField);

        int option = JOptionPane.showConfirmDialog(parent, loginPanel, "Passenger Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String userId = userIdField.getText().trim();
            String password = new String(passwordField.getPassword());
            String passport = passportField.getText().trim();

            if (!userId.matches("^\\d+$")) {
                JOptionPane.showMessageDialog(parent, "Passenger ID must be digits only.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Password cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (passport.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Passport Number cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (db.selectPassenger(userId, password, passport)) {
                currentPassenger = new Passenger();
                currentPassenger.setUserID(userId);
                currentPassenger.setPassword(password);
                currentPassenger.setPassportNumber(passport);
                JOptionPane.showMessageDialog(parent, "Passenger login successful!", "Login Success", JOptionPane.INFORMATION_MESSAGE);
                showPassengerMenu(parent);
            } else {
                JOptionPane.showMessageDialog(parent, "Invalid passenger credentials", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showPassengerMenu(JFrame parent) {
        if (currentPassenger == null) {
            JOptionPane.showMessageDialog(parent, "No passenger logged in.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] options = {"Book Ticket", "View My Bookings", "Logout"};
        while (true) {
            int choice = JOptionPane.showOptionDialog(parent, "Welcome, Passenger " + currentPassenger.getUserID() + "!", "Passenger Panel",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                bookTicketGUI(parent);
            } else if (choice == 1) {
                viewMyBookingsGUI(parent);
            } else {
                currentPassenger.logout();
                currentPassenger = null;
                JOptionPane.showMessageDialog(parent, "Passenger logged out.", "Logout", JOptionPane.INFORMATION_MESSAGE);
                break;
            }
        }
    }

    private void bookTicketGUI(JFrame parent) {
        loadData();

        ArrayList<Flight> availableFlights = new ArrayList<>();
        for (Flight f : flights) {
            if (f.getAvailableSeats() > 0) {
                availableFlights.add(f);
            }
        }

        if (availableFlights.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No flights available for booking.", "Book Ticket", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JComboBox<String> flightComboBox = new JComboBox<>();
        for (Flight f : availableFlights) {
            flightComboBox.addItem(f.toString());
        }

        JTextField seatNumberField = new JTextField(10);

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("Select Flight:"));
        panel.add(flightComboBox);
        panel.add(new JLabel("Enter Seat Number:"));
        panel.add(seatNumberField);

        int option = JOptionPane.showConfirmDialog(parent, panel, "Book Ticket", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            try {
                int selectedIndex = flightComboBox.getSelectedIndex();
                if (selectedIndex == -1) {
                    JOptionPane.showMessageDialog(parent, "Please select a flight.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Flight selectedFlight = availableFlights.get(selectedIndex);

                String seatNumStr = seatNumberField.getText().trim();
                if (seatNumStr.isEmpty()) {
                    JOptionPane.showMessageDialog(parent, "Seat number cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int seatNumber = Integer.parseInt(seatNumStr);

                if (seatNumber <= 0 || seatNumber > selectedFlight.getSeatCapacity()) {
                    JOptionPane.showMessageDialog(parent, "Seat number must be between 1 and " + selectedFlight.getSeatCapacity() + ".", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (!db.isSeatAvailable(Integer.parseInt(selectedFlight.getFlightID()), seatNumber)) {
                    JOptionPane.showMessageDialog(parent, "Seat " + seatNumber + " is already taken on this flight.", "Booking Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Reservation reservation = new Reservation();
                reservation.reservationID = "R" + db.getNextReservationId();
                reservation.passenger = currentPassenger;
                reservation.flight = selectedFlight;
                reservation.seatNumber = String.valueOf(seatNumber);
                reservation.bookingDate = LocalDate.now().toString();
                reservation.status = "confirmed";

                if (db.createReservation(reservation)) {
                    reservations.add(reservation);
                    selectedFlight.updateSeats(-1);
                    JOptionPane.showMessageDialog(parent, "Ticket booked successfully!\nReservation ID: " + reservation.reservationID, "Booking Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(parent, "Failed to book ticket. Please try again.", "Booking Failed", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, "Invalid seat number. Please enter a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "An unexpected error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void viewMyBookingsGUI(JFrame parent) {
        if (currentPassenger == null) {
            JOptionPane.showMessageDialog(parent, "No passenger logged in.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        loadData();
        ArrayList<Reservation> myBookings = new ArrayList<>();
        int currentPassengerId = Integer.parseInt(currentPassenger.getUserID());

        for (Reservation r : reservations) {
            if (Integer.parseInt(r.passenger.userID) == currentPassengerId) {
                myBookings.add(r);
            }
        }

        if (myBookings.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No bookings found for your account.", "My Bookings", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] columnNames = {"Res ID", "Flight ID", "Airline", "Origin", "Destination", "Seat No.", "Booking Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        for (Reservation r : myBookings) {
            Object[] rowData = {
                    r.reservationID,
                    r.flight.getFlightID(),
                    r.flight.getAirlineName(),
                    r.flight.getOrigin(),
                    r.flight.getDestination(),
                    r.seatNumber,
                    r.bookingDate,
                    r.status
            };
            model.addRow(rowData);
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 400));

        JOptionPane.showMessageDialog(parent, scrollPane, "Your Bookings", JOptionPane.PLAIN_MESSAGE);
    }

    private boolean isValidTime(String time) {
        return Pattern.matches("^([01]\\d|2[0-3]):[0-5]\\d$", time);
    }

    private boolean isValidCityName(String name) {
        return Pattern.matches("^[a-zA-Z ]+$", name);
    }
}