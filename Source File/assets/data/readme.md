# VRBS - Cursor Guide

## Purpose
This file helps the Cursor agent understand the system quickly and implement efficiently.

---

## Tech Stack
- Java (Backend)
- JavaFX (Frontend)
- MongoDB (Database)

---

## Roles
- User
- Driver
- Supplier
- Admin (default login only)

---

## Key Modules
1. Authentication
2. Vehicle Management
3. Booking System
4. Rental System
5. Payment & Penalty
6. Chat Support
7. Reports

---

## Folder Structure (Recommended)

/src
  /controllers
  /services
  /models
  /dao
  /utils
/ui
  /pages
  /components
/assets
/config

---

## Execution Flow
1. Launch app
2. Login/Register
3. Redirect to role dashboard
4. Perform actions
5. Store/retrieve from DB

---

## Important Notes
- Use MVC pattern
- Keep DAO separate
- Use async UI updates
- Avoid blocking UI thread

---

## UI Design Rules
- Modern layout
- Animations enabled
- No lag
- Clean spacing

---

## Database Rules
- Use collections properly
- Avoid duplication
- Maintain relations via IDs

---

## Security Rules
- Always hash passwords
- Never expose credentials
- Validate all inputs
