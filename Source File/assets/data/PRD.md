# PRODUCT REQUIREMENTS DOCUMENT (PRD)
## Vehicle Rental and Booking System (VRBS)

## 1. Product Overview
VRBS is a standalone desktop web-style application built using:
- Backend: Java
- Frontend: JavaFX
- Database: MongoDB

Core Features:
- Vehicle rental (hourly)
- Cab booking (with driver)
- Role-based system (User, Driver, Supplier, Admin)
- Secure authentication (hashing + rehashing)
- Real-time booking, penalties, and tracking

---

## 2. Actors

### 2.1 User
- Register / Login
- Rent vehicles (hourly)
- Book cab with driver
- View history
- Pay penalties
- Chat support

### 2.2 Driver
- Register / Login
- Accept/reject bookings
- View assigned rides
- Update ride status

### 2.3 Supplier
- Register / Login
- Add/manage vehicles
- View performance

### 2.4 Admin
- Default login:
  - username: admin
  - password: admin123
- No registration allowed
- Full system control

---

## 3. Core Functionalities

### 3.1 Authentication
- BCrypt hashing for passwords
- Rehash on login validation
- Max 3 login attempts → account lock
- Role-based access control

---

### 3.2 Vehicle Management
- Add/Edit/Delete vehicles
- Status:
  - AVAILABLE
  - BOOKED
  - RENTED
  - MAINTENANCE
- Linked to supplier

---

### 3.3 Booking System (Cab)
Flow:
1. User selects pickup & drop
2. System assigns nearest driver
3. Driver accepts/rejects
4. Status flow:
   - Requested → Accepted → Arrived → Completed

---

### 3.4 Rental System
Flow:
1. Select vehicle
2. Choose duration (1–24 hrs)
3. Add to cart (max 5)
4. Confirm booking
5. Payment

Pricing:
- Base rate
- Peak multiplier (1.5x)
- Late penalty ₹50/hour after 1 hr grace

---

### 3.5 Payment System
- Multiple payments supported
- Deposit + full payment
- Refund logic:
  - Full refund <15 min
  - 50% after 15 min

---

### 3.6 Penalty System
- Auto-calculated on late return
- Blocks user if unpaid
- Admin investigation flag

---

### 3.7 Driver Assignment
- One active assignment per driver
- History tracking maintained

---

### 3.8 Chat Support System
- Floating UI component
- Predefined 10 Q&A
- Instant responses

---

### 3.9 Notifications
- Booking confirmation
- Payment updates
- Reminders

---

### 3.10 Reports (Admin)
- Revenue
- Vehicle usage
- Late returns
- Driver performance

---

## 4. Database Entities (MongoDB Collections)

- Users
- Drivers
- Suppliers
- Vehicles
- VehicleTypes
- Bookings
- Rentals
- Payments
- Refunds
- Feedback
- Notifications
- PromoCodes
- AuditLogs
- Sessions

---

## 5. UI/UX Requirements

### Navigation Bar
- Home
- Rent
- Book Cab
- Vehicles
- About
- Contact
- Profile

### Hero Section
- Full-width banner
- Vehicle slider
- CTA buttons:
  - Rent Now
  - Book Cab

---

### UI Features
- Smooth animations (no lag)
- Horizontal + vertical scrolling
- Card-based layouts
- Hover effects
- Responsive resizing
- Modal windows with maximize/minimize

---

## 6. Security

- Password hashing (BCrypt)
- No plaintext password display
- Session timeout (15 min)
- Audit logs for all actions

---

## 7. Constraints

- Max 5 vehicles per cart
- Max 24 hours rental
- Single active rental per vehicle
- Driver must be available

---

## 8. Performance

- Login < 1 sec
- Booking < 2 sec
- Availability check < 0.5 sec

---

## 9. System Flow (Simplified)

User Flow:
Login → Dashboard → Select Service → Book/Rent → Payment → Confirmation

Driver Flow:
Login → View Requests → Accept → Complete

Admin Flow:
Login → Dashboard → Manage System

---

## 10. Architecture

Frontend (JavaFX)
        ↓
Backend (Java Services)
        ↓
DAO Layer
        ↓
MongoDB

---

## 11. Non-Functional Requirements

- Reliability: ACID-like behavior
- Availability: Local system uptime
- Maintainability: Modular code
- Scalability: Extendable collections
