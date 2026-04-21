# Rento — UML Diagrams

## 1. Class Diagram (Domain Model)

```mermaid
classDiagram
    class User {
        +ObjectId id
        +String fullName
        +String email
        +String phone
        +String address
        +String password
        +String driverLicenseNumber
        +Role role
        +boolean verified
        +int age
        +double walletBalance
        +Date createdAt
        +Date updatedAt
    }
    class Role {
        <<enumeration>>
        GUEST
        USER
        DRIVER
        SUPPLIER
        ADMIN
    }

    class Vehicle {
        +ObjectId id
        +String vin
        +String make
        +String model
        +int year
        +String licensePlate
        +Category category
        +FuelType fuelType
        +Status status
        +double dailyRate
        +int seats
        +String color
        +ObjectId ownerId
        +String branchLocation
        +ApprovalStatus approvalStatus
        +double currentMileage
        +double nextServiceDue
        +Date createdAt
        +Date updatedAt
        +boolean needsMaintenance()
        +String getDisplayName()
    }
    class VehicleCategory {
        <<enumeration>>
        SEDAN
        SUV
        HATCHBACK
        COUPE
        TRUCK
        VAN
        BIKE
        BUS
    }
    class VehicleStatus {
        <<enumeration>>
        AVAILABLE
        RESERVED
        IN_USE
        UNDER_INSPECTION
        MAINTENANCE
        UNAVAILABLE
    }
    class ApprovalStatus {
        <<enumeration>>
        PENDING
        APPROVED
        REJECTED
    }

    class Booking {
        +ObjectId id
        +ObjectId userId
        +ObjectId vehicleId
        +ObjectId driverId
        +ObjectId preferredDriverId
        +String pickupLocation
        +String dropoffLocation
        +Date pickupDateTime
        +Date returnDateTime
        +double totalCost
        +double depositAmount
        +double taxAmount
        +double discountApplied
        +BookingStatus status
        +String otp
        +boolean otpVerified
        +Date createdAt
        +Date updatedAt
        +long getRentalDays()
    }
    class BookingStatus {
        <<enumeration>>
        PENDING
        CONFIRMED
        IN_PROGRESS
        COMPLETED
        CANCELLED
    }

    class Rental {
        +ObjectId id
        +ObjectId vehicleId
        +ObjectId supplierId
        +ObjectId renterId
        +double pricePerDay
        +Date startDate
        +Date endDate
        +RentalStatus status
        +String approvalOtp
        +boolean otpVerified
        +double totalAmount
        +double penaltyAmount
        +Date requestedAt
        +Date approvedAt
        +Date completedAt
        +boolean isOverdue()
    }
    class RentalStatus {
        <<enumeration>>
        REQUESTED
        APPROVED
        ACTIVE
        COMPLETED
        OVERDUE
        REJECTED
        CANCELLED
    }

    class Payment {
        +ObjectId id
        +ObjectId bookingId
        +ObjectId userId
        +double amount
        +double taxAmount
        +double discountAmount
        +double totalAmount
        +PaymentMethod paymentMethod
        +PaymentStatus status
        +String cardNumber
        +String transactionRef
        +String currency
        +Date paymentDate
        +Date createdAt
    }
    class PaymentStatus {
        <<enumeration>>
        PENDING
        PROCESSING
        COMPLETED
        FAILED
        REFUNDED
    }
    class PaymentMethod {
        <<enumeration>>
        CREDIT_CARD
        DEBIT_CARD
        UPI
        NET_BANKING
        WALLET
    }

    User "1" --> "0..*" Booking : places
    User "1" --> "0..*" Rental : requests
    User "1" --> "0..*" Payment : makes
    Vehicle "1" --> "0..*" Booking : reserved by
    Vehicle "1" --> "0..*" Rental : rented via
    Booking "1" --> "0..1" Payment : paid through
    User --> Role
    Vehicle --> VehicleCategory
    Vehicle --> VehicleStatus
    Vehicle --> ApprovalStatus
    Booking --> BookingStatus
    Rental --> RentalStatus
    Payment --> PaymentStatus
    Payment --> PaymentMethod
```

---

## 2. Sequence Diagram — Booking Flow

```mermaid
sequenceDiagram
    actor Customer
    actor Driver
    participant BookingService
    participant VehicleDAO
    participant UserDAO
    participant BookingDAO
    participant NotificationService
    participant PaymentService

    Customer->>BookingService: createBooking(userId, vehicleId, pickup, dropoff, dates)
    BookingService->>VehicleDAO: findById(vehicleId)
    VehicleDAO-->>BookingService: Vehicle
    BookingService->>UserDAO: findById(userId)
    UserDAO-->>BookingService: Customer
    BookingService->>BookingService: calculatePricing()
    BookingService->>BookingDAO: insertBooking(booking) [status=PENDING, OTP generated]
    BookingDAO-->>BookingService: success
    BookingService->>UserDAO: adjustWalletBalance(-deposit)
    BookingService->>VehicleDAO: updateStatus(RESERVED)
    BookingService->>NotificationService: notify(Customer, "Booking created")
    BookingService->>NotificationService: notify(Driver, "New booking assigned")
    BookingService-->>Customer: Booking

    Customer->>PaymentService: processPayment(bookingId, paymentDetails)
    PaymentService-->>Customer: Payment confirmed
    PaymentService->>NotificationService: notify(Customer, "Payment received")
    PaymentService->>NotificationService: notify(Driver, "Payment completed, OTP ready")

    Driver->>BookingService: confirmBooking(bookingId, driverId)
    BookingService->>BookingDAO: updateBooking(status=CONFIRMED)
    BookingService->>NotificationService: notify(Customer, "Driver confirmed, OTP: XXXX")
    BookingService-->>Driver: confirmed

    Customer->>Driver: Share OTP verbally at pickup
    Driver->>BookingService: verifyRideOtp(driverId, otp)
    BookingService->>BookingDAO: updateBooking(status=IN_PROGRESS, otpVerified=true)
    BookingService-->>Driver: verified

    Driver->>BookingService: completeBooking(bookingId)
    BookingService->>BookingDAO: updateBooking(status=COMPLETED)
    BookingService->>VehicleDAO: updateStatus(UNDER_INSPECTION)
    BookingService->>UserDAO: adjustWalletBalance(Driver, +15% commission)
    BookingService->>NotificationService: notify(Customer, "Receipt generated")
    BookingService-->>Driver: completed
```

---

## 3. Sequence Diagram — Rental Flow (Supplier)

```mermaid
sequenceDiagram
    actor User
    actor Supplier
    participant RentalService
    participant VehicleDAO
    participant RentalDAO
    participant NotificationService

    User->>RentalService: requestRental(userId, vehicleId, startDate, endDate)
    RentalService->>VehicleDAO: findById(vehicleId)
    VehicleDAO-->>RentalService: Vehicle (AVAILABLE)
    RentalService->>RentalDAO: insertRental(status=REQUESTED)
    RentalDAO-->>RentalService: success
    RentalService->>NotificationService: notify(Supplier, "New rental request")
    RentalService-->>User: Rental request submitted

    Supplier->>RentalService: approveRental(rentalId) [OTP generated]
    RentalService->>VehicleDAO: updateStatus(RESERVED)
    RentalService->>RentalDAO: updateRental(status=APPROVED)
    RentalService->>NotificationService: notify(User, "Rental approved, OTP: XXXX")
    RentalService-->>Supplier: approved

    User->>RentalService: verifyOtp(rentalId, otp)
    RentalService->>RentalDAO: updateRental(status=ACTIVE, otpVerified=true)
    RentalService->>VehicleDAO: updateStatus(IN_USE)
    RentalService-->>User: rental active

    User->>RentalService: completeRental(rentalId)
    RentalService->>RentalDAO: updateRental(status=COMPLETED)
    RentalService->>VehicleDAO: updateStatus(AVAILABLE)
    RentalService-->>User: rental completed
```

---

## 4. Application Flow (Use-Case Overview)

```mermaid
flowchart TD
    Start([Launch App]) --> Landing[Landing Screen]
    Landing --> Login[Login]
    Landing --> Register[Register]

    Login -->|success| RoleCheck{User Role?}
    Register -->|success| Login

    RoleCheck -->|USER| UserDash[User Dashboard\nBrowse & Book Vehicles]
    RoleCheck -->|DRIVER| DriverDash[Driver Dashboard\nManage Assigned Bookings]
    RoleCheck -->|SUPPLIER| SupplierDash[Supplier Dashboard\nManage Fleet & Rentals]
    RoleCheck -->|ADMIN| AdminDash[Admin Dashboard\nPlatform Operations & Exports]

    UserDash --> BrowseVehicles[Browse Vehicles]
    BrowseVehicles --> BookVehicle[Create Booking]
    BookVehicle --> Payment[Make Payment]
    Payment --> WaitConfirm[Await Driver Confirmation]
    WaitConfirm --> RideStart[Share OTP → Ride Starts]
    RideStart --> RideComplete[Ride Completed → Receipt]

    UserDash --> RentVehicle[Request Rental from Supplier]
    RentVehicle --> AwaitApproval[Await Supplier Approval]
    AwaitApproval --> VerifyRentalOTP[Verify OTP → Rental Active]

    DriverDash --> ViewPendingBookings[View Pending Bookings]
    ViewPendingBookings --> ConfirmBooking[Confirm Booking]
    ConfirmBooking --> VerifyOTP[Verify Customer OTP]
    VerifyOTP --> CompleteRide[Complete Ride]

    SupplierDash --> ManageFleet[Add / Update Vehicles]
    SupplierDash --> ApproveRentals[Approve / Reject Rentals]
    ApproveRentals --> IssueOTP[Issue Rental OTP]

    AdminDash --> ManageUsers[View & Manage Users]
    AdminDash --> ApproveVehicles[Approve Vehicle Listings]
    AdminDash --> ExportReports[Export Reports PDF/CSV]
```
