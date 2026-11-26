---
name: Slow Down
colors:
  surface: '#f3fcf1'
  surface-dim: '#d4dcd2'
  surface-bright: '#f3fcf1'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#edf6eb'
  surface-container: '#e8f0e6'
  surface-container-high: '#e2ebe0'
  surface-container-highest: '#dce5da'
  on-surface: '#161d17'
  on-surface-variant: '#424842'
  inverse-surface: '#2a322b'
  inverse-on-surface: '#ebf3e8'
  outline: '#727972'
  outline-variant: '#c2c8c0'
  surface-tint: '#47654e'
  primary: '#36533c'
  on-primary: '#ffffff'
  primary-container: '#4d6b53'
  on-primary-container: '#c8eacc'
  inverse-primary: '#aecfb2'
  secondary: '#4f6353'
  on-secondary: '#ffffff'
  secondary-container: '#cfe5d1'
  on-secondary-container: '#546757'
  tertiary: '#793a23'
  on-tertiary: '#ffffff'
  tertiary-container: '#975138'
  on-tertiary-container: '#ffdace'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c9ebcd'
  primary-fixed-dim: '#aecfb2'
  on-primary-fixed: '#04210e'
  on-primary-fixed-variant: '#304d37'
  secondary-fixed: '#d2e8d4'
  secondary-fixed-dim: '#b6ccb9'
  on-secondary-fixed: '#0d1f13'
  on-secondary-fixed-variant: '#384b3c'
  tertiary-fixed: '#ffdbcf'
  tertiary-fixed-dim: '#ffb59c'
  on-tertiary-fixed: '#380c00'
  on-tertiary-fixed-variant: '#72351e'
  background: '#f3fcf1'
  on-background: '#161d17'
  surface-variant: '#dce5da'
typography:
  display-hero:
    fontFamily: Newsreader
    fontSize: 44px
    fontWeight: '400'
    lineHeight: 52px
    letterSpacing: -0.02em
  display-hero-mobile:
    fontFamily: Newsreader
    fontSize: 36px
    fontWeight: '400'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Newsreader
    fontSize: 30px
    fontWeight: '500'
    lineHeight: 38px
  headline-md:
    fontFamily: Newsreader
    fontSize: 24px
    fontWeight: '500'
    lineHeight: 32px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '700'
    lineHeight: 26px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
  metric-display:
    fontFamily: JetBrains Mono
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.03em
  metric-label:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system establishes a gentle, restorative digital sanctuary designed to intercede during habitual doomscrolling. Rather than punishing screen time or triggering guilt, the visual demeanor acts as an empathetic, tactile companion that invites users to exhale, pause, and ground themselves in the tangible world.

### Design Movement & Aesthetic
The style synthesizes **Tactile Organic Warmth** with **Material 3 Expressive**. It rejects mechanical sterility, clinical grey palettes, and harsh warning dialogs in favor of:
- **Paper & Clay Metaphors:** Unbleached linen surfaces, warm ceramic pills, and soft botanical hues.
- **Biomorphic Fluidity:** Pebble and squircle geometries that evoke smooth river stones and organic rhythm.
- **Rhythmic Micro-Interactions:** Subtle, continuous breathing cycles across throttled states that match relaxed human respiration (4-second inhale, 4-second exhale cycles).

## Colors

The palette avoids alarming reds, cold electric blues, or high-saturation purples. It relies on a grounding forest-botanical ecosystem anchored by unbleached linen and warm terracotta accents.

- **Primary (`#4D6B53` - Forest Sage):** Represents conscious presence, grounded restoration, and active protective intervention. Used for primary throttle controls, active toggles, and hero action states.
- **Secondary (`#8A9F8D` - Soft Eucalyptus):** Represents ambient calm and non-urgent tracking. Used for secondary pill chips, relaxed surface containers, and passive progress indicators.
- **Tertiary (`#C97A5E` - Warm Terracotta/Clay):** Represents gentle friction, throttled network thresholds, and warning-free time cues. Serves as a warm, human, non-punitive indicator when limits approach.
- **Neutral (`#2B332C` - Deep Charcoal Forest):** Deep organic slate-forest providing high-contrast text legibility without the harshness of pure `#000000`.

### Surface & Canvas Architecture
- **App Background Canvas:** `#F8F6F0` (Linen White), providing a warm, paper-like tactile feel.
- **Surface Container Lowest:** `#FFFFFF` (pure soft card highlights).
- **Surface Container Low:** `#F2EFE8` (Oatmeal tint).
- **Surface Container Normal:** `#ECE7DE` (Sunbaked clay/sandstone tint for standard cards).
- **Surface Container High:** `#E4DED3` (Throttled card states and recessed wells).
- **Surface Container Highest:** `#DDD6C9` (Dividers, structural bounds, and chip tracks).

## Typography

The typographic hierarchy harmonizes meditative editorial prose with precise instrumentation:

- **Hero & Pause States (`Newsreader`):** An organic, literary serif that signals stillness, contemplative breathing, and reflection. Used for screen titles, breathing exercise prompts, and mindful intervention messages.
- **Body & Controls (`Plus Jakarta Sans`):** A friendly, contemporary geometric sans with soft apertures and humanist forms that prevents clinical friction in descriptions, settings, and button actions.
- **Data & Telemetry (`JetBrains Mono`):** Monospaced, highly legible numeric rendering for active bandwidth meters (kbps), session countdowns, sleep timers, and protocol logs.

## Layout & Spacing

The layout is built for native Android edge-to-edge experiences using Jetpack Compose window insets:

### Insets & Edge-to-Edge Rules
- **Status Bar & Navigation:** Transparent system bars. Canvas draws behind all system bars with `WindowInsets.safeDrawing` or `systemBarsPadding()` applied only to actionable contents.
- **Touch Target Floor:** All interactive controls (steppers, toggles, dismiss handles, chips) must enforce a minimum physical bounding box of `48.dp x 48.dp`, even if visual geometry is smaller.
- **Rhythm Model:** Layout follows an 8dp vertical baseline grid with 4dp increments for dense telemetry rows. Vertical rhythm prioritizes spacious padding around throttled states to induce unhurried focus.
- **Screen Margins:** Fixed `20.dp` (`margin: 1.25rem`) padding on left and right edges for standard mobile viewports, scaling up to `24.dp` gutters on foldables and tablets.

## Elevation & Depth

Visual depth is achieved through **tonal surface stacking and tactile frosted layers**, entirely eliminating dark drop shadows.

- **Tonal Stepping:** Layering uses natural pigment density. Base screens sit at `#F8F6F0`, primary cards at `#ECE7DE`, and nested active controls at `#F2EFE8`. Higher interactive focus is designated by luminance rather than shadow projection.
- **Pebble Frost (Compose Blur):** Modal pause sheets and floating metrics bars employ a custom background frosted blur (`16.dp` RenderEffect blur with a 65% alpha tint of `#F8F6F0`).
- **Tactile Insets:** Well containers and inactive progress tracks use recessed inner tones (`#E4DED3`) without directional bevels, conveying soft-pressed clay.

## Shapes

The shape system adopts smooth, organic curvature reminiscent of hand-smoothed pebbles, river rocks, and ergonomic gripware:

- **Pebble Cards & Modals:** Standard cards use `16.dp` (`rounded-lg`) corner radii; hero intervention sheets and pause modals expand to `24.dp` (`rounded-xl`).
- **Pill Primitives:** Buttons, telemetry pills, and status tags are fully rounded (`CircleShape` / `9999.dp`).
- **Fluid Wave Silhouettes:** Real-time throttling meters utilize sine-wave bezier curves with smooth continuous radii, avoiding sharp angular data graphs.

## Components

### Buttons & Action Bars
- **Primary Rest Action:** Rounded pill container with `#4D6B53` fill and `#FFFFFF` text. Includes an internal micro-spring press scale (`0.97f`) on Android haptic feedback (`HapticFeedbackType.TextHandleMove`).
- **Secondary / Pause Action:** Muted eucalyptus background (`#8A9F8D` at 20% opacity) with `#2B332C` label.
- **Ghost Action:** Borderless pill with `16.dp` horizontal padding, using `body-md` in Forest Slate.

### Live Bandwidth Meter & Throttle Chips
- **Container:** Frosted floating pebble capsule (`JetBrains Mono` text).
- **States:**
  - *Unrestricted:* Neutral oat tint with subtle Forest Sage dot indicator.
  - *Throttling Active:* Soft Terracotta outline (`#C97A5E`), pulsating mono metric showing real-time deceleration (e.g., `128 kbps ↓`).

### Breathing Intervention Card
- **Surface:** `#ECE7DE` card with `24.dp` radius.
- **Visual Core:** Animated organic ripple circle that scales smoothly over 4-second intervals, guiding the user's inhale/exhale sequence without sudden visual alerts.

### Sliders & Steppers (Friction Controls)
- **Track:** Thick `12.dp` rounded track in `#DDD6C9`. Active filled range takes `#C97A5E` (Terracotta) to signify physical resistance.
- **Thumb:** Large tactile pebble thumb (`32.dp x 24.dp` squircle) with deep forest tactile grip lines.

### Inputs & Selection Controls
- **Checkboxes & Radios:** Pill-shaped toggle bars or rounded-circle checkboxes (`#4D6B53` check glyph, `#E4DED3` unselected container) strictly padded within minimum `48.dp` click boundaries.
- **Input Fields:** Soft clay well backgrounds (`#F2EFE8`), no visible underline, subtle `1.5.dp` Forest Sage outline strictly on focus.