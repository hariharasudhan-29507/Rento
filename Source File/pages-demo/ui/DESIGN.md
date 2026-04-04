# Design System: The Obsidian Kinetic Framework

## 1. Overview & Creative North Star
**Creative North Star: "The Architectural Concierge"**

This design system rejects the "utility-first" clutter typical of the rental industry. Instead, it adopts a high-end editorial approach that treats vehicle selection like a gallery experience. We move beyond the "template" look by utilizing **Bento Grid modularity** paired with **Tonal Depth**. 

The system is defined by its intentional asymmetry and "quiet luxury." By leveraging deep, emerald-infused blacks and mist-like light surfaces, we create an environment that feels premium yet grounded. We don't just facilitate a transaction; we provide a curated transition from "where you are" to "where you need to be."

## 2. Colors & Atmospheric Depth

The palette is rooted in the "Emerald/Black" spectrum, designed to evoke the sleekness of a night-time city drive.

### The "No-Line" Rule
**Strict Mandate:** Traditional 1px solid borders are prohibited for sectioning. 
Structure is achieved through **Background Color Shifts**. To separate a sidebar from a main feed, transition from `surface` (#0a1514) to `surface-container-low` (#131e1c). This creates a sophisticated, "molded" look rather than a fragmented one.

### Surface Hierarchy & Nesting
Treat the UI as a physical stack of materials. 
*   **Base Layer:** `surface` (#0a1514)
*   **Sectional Shifts:** Use `surface-container` (#172220) to define large content areas.
*   **Active Modules (Bento Boxes):** Use `surface-container-high` (#212c2b) for standard cards.
*   **Interaction Focus:** Use `inverse-surface` (#d9e5e3) only for critical high-contrast modules (like booking summaries or promotional highlights) to snap the user's attention.

### The "Glass & Gradient" Rule
To avoid a flat "flat-design" trap, apply a subtle **Glassmorphism** effect to floating elements (navigation bars, modal overlays). Use `surface-bright` (#303b3a) at 60% opacity with a `24px` backdrop blur. 
**Signature Texture:** For primary CTAs, use a linear gradient from `primary` (#8bd5b9) to `primary-container` (#559e84) at 135 degrees. This provides a tactile "glow" that feels engineered and professional.

## 3. Typography: The Editorial Voice

We utilize a dual-font strategy to balance character with high-performance legibility.

*   **Headings (Poppins):** Selected for its geometric precision. Use `display-lg` and `headline-md` with tightened letter-spacing (-0.02em) to create an authoritative, editorial feel. Poppins represents the "luxury" aspect of the service.
*   **Body (Inter):** The workhorse. Inter is used for all functional data (car specs, pricing, legalese). It provides the "practicality" required for a rental service.

**Typographic Hierarchy:**
*   **Display/Headline:** Use `on-surface` (#d9e5e3) for maximum drama against the dark background.
*   **Body:** Use `on-surface-variant` (#bec9c3) for long-form reading to reduce eye strain.
*   **Action Labels:** Use `on-primary-fixed` (#002117) on light buttons to ensure a "stamped" high-contrast look.

## 4. Elevation & Depth

### The Layering Principle
Depth is achieved through "Tonal Stacking." Place a `surface-container-highest` card on top of a `surface-container-low` background. This creates a natural "lift" based on color theory rather than drop shadows, keeping the UI clean and modern.

### Ambient Shadows
When an element must float (e.g., a car selection popup), use **Ambient Shadows**:
*   **Blur:** 40px to 60px.
*   **Opacity:** 6% - 10%.
*   **Color:** Use a tinted shadow based on `#002117` rather than pure black. This makes the shadow feel like a natural part of the emerald environment.

### The "Ghost Border" Fallback
If a border is required for accessibility (e.g., input fields), use the **Ghost Border**: `outline-variant` (#3f4944) at 20% opacity. Never use 100% opaque lines.

## 5. Components

### Bento Cards & Lists
*   **Guideline:** No divider lines. Use `1.5rem` (24px) of vertical whitespace to separate list items. 
*   **Styling:** Use `xl` (0.75rem) corner radius for all cards to create a modern, "tech-hardware" feel.

### Buttons
*   **Primary:** Gradient-filled (`primary` to `primary-container`), no border, `full` (pill) or `xl` (0.75rem) radius.
*   **Secondary:** `surface-container-highest` background with `on-surface` text.
*   **Tertiary:** Text-only with an underline that appears only on hover, using the `primary` color.

### Input Fields
*   **Surface:** `surface-container-low`.
*   **State:** On focus, the background shifts to `surface-container-high` with a 1px "Ghost Border" of `primary` (#8bd5b9).
*   **Typography:** Labels must be `label-sm` in `on-surface-variant`.

### Imagery Integration
*   **Content:** High-resolution photography of modern Sedans, SUVs, and Minivans. 
*   **Treatment:** Images should be placed within Bento cells. Use a subtle `0.5px` inner glow (white at 5% opacity) on the top edge of image containers to simulate a glass cover.

## 6. Do's and Don'ts

### Do
*   **Use Asymmetry:** In the Bento grid, vary the widths of cells (e.g., 60% / 40%) to create a dynamic, editorial flow.
*   **Embrace Negative Space:** Allow car images to "breathe" with generous padding (`32px+`) inside their containers.
*   **Tonal Transitions:** Use background shifts to guide the eye from the navigation to the content.

### Don't
*   **No Supercars:** Do not use Ferraris or Lamborghinis. This system is for "Emerald Velocity"—a service for high-end, practical daily transport. Stick to modern Teslas, Audis, or Volvos.
*   **No Pure Black:** Never use `#000000`. The depth comes from the `surface` (#0a1514) emerald-tinted black.
*   **No Heavy Borders:** Avoid 1px solid lines; they "break" the architectural flow of the layers.