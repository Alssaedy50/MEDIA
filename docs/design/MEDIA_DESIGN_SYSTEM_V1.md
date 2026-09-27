# MEDIA Design System v1.0

**Product:** MEDIA — Medical Education AI  
**Platform:** Android Mobile  
**Design System Version:** 1.0  
**Status:** Foundation / Source of Truth  
**Primary Design Frame:** 390 × 844 dp

---

## 1. Purpose

This document defines the visual identity, UI foundations, reusable components, interaction states, accessibility rules, and visual constraints for the MEDIA Android application.

It is the **visual source of truth** for MEDIA.

All future screens, components, mockups, prototypes, and Android implementations must follow this document unless an explicitly approved design-system revision supersedes it.

The design system controls the **presentation and interaction layer**.

It must not alter or replace:
- AI models
- AI reasoning
- Knowledge Engine
- Knowledge Repository
- Medical content
- Retrieval logic
- Verification logic
- Safe-abstention behavior
- Existing working data flows

---

## 2. Product Design Direction

MEDIA is a medical education application, not a generic chatbot.

The interface should communicate:
- Medical
- Academic
- Professional
- Calm
- Trustworthy
- Modern
- Focused
- Offline-first
- Content-first

### Visual Principle

> Content first. The interface supports studying; it must never compete with medical content.

### Avoid

- Excessive gradients
- Neon colors
- Glassmorphism
- Excessive cards
- Decorative animations
- Excessive illustrations
- Random colors
- Multiple unrelated icon styles
- Large decorative empty areas
- Game-like visual effects
- Fake statistics or decorative data
- White/light UI surfaces that reduce readability

---

## 3. Base Layout

### Reference Frame

- Width: **390dp**
- Height: **844dp**

The application must remain responsive on other Android screen sizes. The 390 × 844 frame is only the visual reference frame.

### Screen Padding

- Standard: **20dp**
- Compact: **16dp**

### Safe Area

All important content must respect:
- Status bar inset
- Navigation bar / gesture inset
- Keyboard inset
- Display cutouts

No important UI element may be hidden behind system bars.

---

## 4. Color Tokens

### Core Background

| Token | Value | Usage |
|---|---|---|
| `media.color.background` | `#0B1117` | Main application background |
| `media.color.surface` | `#121A22` | Standard surfaces |
| `media.color.surfaceVariant` | `#18232D` | Secondary surfaces |
| `media.color.surfaceElevated` | `#1C2934` | Dialogs, sheets, elevated content |

Visual hierarchy:

```
Background
    ↓
Surface
    ↓
Surface Variant
    ↓
Surface Elevated
```

### Primary Medical Accent

```
media.color.primary       = #39C6A2
media.color.primaryHover  = #4DD1B0
media.color.primaryPressed = #2EAA8B
media.color.primarySoft   = #163A35
```

Use the primary color for active navigation, primary actions, focus indicators, cursor, important links, selected states, and key interactive medical UI elements.

Do not use the primary color everywhere.

### Text

```
media.color.text.primary   = #F3F7F6
media.color.text.secondary = #A9B7B5
media.color.text.tertiary  = #74837F
media.color.text.disabled  = #4D5A58
```

### Semantic Colors

```
media.color.success       = #43C98B
media.color.successSoft   = #15382E

media.color.warning       = #E8B95B
media.color.warningSoft   = #3A301B

media.color.error         = #F06C72
media.color.errorSoft     = #3B2025

media.color.info          = #63A9E8
media.color.infoSoft      = #1B3040
```

Semantic colors must not be used decoratively.

### Borders

```
media.color.border.subtle  = #22303A
media.color.border.default = #2A3944
media.color.border.focus   = #39C6A2
```

Borders must be subtle. Avoid thick borders around every component.

---

## 5. Offline Design Rule

Offline does not automatically mean error.

Normal state:

> Offline • Local resources available

Actual failure:

> Unable to load resource

The application must clearly distinguish **Offline** from **Error**.

---

## 6. Typography

### Primary Latin Font

**Inter**

### Arabic Font / Fallback

**Noto Sans Arabic**

The UI must support mixed English medical terminology and Arabic explanations.

### Typography Scale

| Style | Size | Weight | Line Height |
|---|---:|---:|---:|
| Display Large | 32sp | 700 | 40sp |
| H1 | 28sp | 700 | 36sp |
| H2 | 22sp | 700 | 30sp |
| H3 | 18sp | 600 | 26sp |
| Body Large | 17sp | 400 | 26sp |
| Body | 16sp | 400 | 24sp |
| Body Medium | 15sp | 500 | 22sp |
| Secondary | 14sp | 400 | 20sp |
| Caption | 12sp | 500 | 16sp |

### Medical Content Typography

- Answer title: **20sp / 700**
- Section heading: **17sp / 600**
- Medical body: **16sp / 400 / 26sp line height**
- Important definition: **16sp / 500 / 26sp**
- Source: **13sp / 400**

Medical terminology may use the primary accent selectively. Do not color entire paragraphs.

---

## 7. Spacing Tokens

```
media.spacing.4  = 4dp
media.spacing.8  = 8dp
media.spacing.12 = 12dp
media.spacing.16 = 16dp
media.spacing.20 = 20dp
media.spacing.24 = 24dp
media.spacing.32 = 32dp
media.spacing.40 = 40dp
media.spacing.48 = 48dp
media.spacing.64 = 64dp
```

Usage:
- 4dp: micro spacing
- 8dp: icon/text spacing
- 12dp: compact spacing
- 16dp: standard component padding
- 20dp: screen padding
- 24dp: section spacing
- 32dp: major section spacing
- 40dp: hero spacing
- 48dp: large separation
- 64dp: major empty-state spacing

---

## 8. Corner Radius

```
media.radius.small  = 8dp
media.radius.medium = 12dp
media.radius.large  = 16dp
media.radius.xl     = 20dp
media.radius.pill   = 999dp
```

Standard usage:
- Buttons: 12dp
- Inputs: 16dp
- Cards: 16dp
- Bottom sheets: 24dp top corners
- Chips: 999dp

---

## 9. Elevation

```
Level 0 = 0dp
Level 1 = 2dp
Level 2 = 4dp
Level 3 = 8dp
```

Prefer surface contrast over strong shadows. Do not use large glowing shadows.

---

## 10. Icon System

Use one consistent icon family.

Style:
- Outline
- Rounded
- Consistent stroke
- Consistent visual density

Sizes:
- 16dp: inline
- 20dp: secondary
- 24dp: standard
- 28dp: prominent
- 32dp: feature

Do not mix filled, outline, 3D, emoji, or unrelated icon families inside the same navigation/component system.

---

## 11. Bottom Navigation

Items:
1. Home
2. Library
3. History
4. More

Approximate height: **80dp** plus required system navigation inset.

- Icon: 24dp
- Label: 12sp

Active:
- Icon: Primary
- Label: Primary

Inactive:
- Icon: Text Tertiary
- Label: Text Tertiary

Background: Surface

Never use a white bottom navigation bar in the dark theme.

---

## 12. Top App Bar

- Height: **64dp**
- Horizontal padding: **20dp**
- Navigation icon: **24dp**
- Title: **20sp / 600**
- Actions: **24dp**

Avoid excessive actions.

---

## 13. Primary Button

- Height: **52dp**
- Minimum width: **120dp**
- Radius: **12dp**

Normal:
- Background: `#39C6A2`
- Text: `#07120F`

Pressed:
- Background: `#2EAA8B`

Disabled:
- Background: `#26342F`
- Text: `#64716D`

Loading:
Keep the button visible and show a subtle progress indicator.

---

## 14. Secondary Button

- Height: **52dp**
- Radius: **12dp**
- Background: Surface or transparent
- Border: Default border
- Text: Primary text

---

## 15. Destructive Button

Use only for destructive actions:
- Delete
- Clear history
- Clear cache
- Logout when confirmation is required

Red must not be used decoratively.

---

## 16. Medical Question Input

This is one of the most important MEDIA components.

- Height: **56dp**
- Radius: **16dp**
- Horizontal padding: **16dp**
- Background: Surface
- Placeholder: `Ask a medical question...`
- Placeholder color: Text Secondary
- Input text: Text Primary
- Cursor: Primary
- Send icon: 24dp

States:
- Empty
- Focused
- Typing
- Sending
- Disabled
- Error

Rules:
- Text must always be readable.
- Cursor must remain visible.
- Placeholder must never overlap cursor/text.
- Focus must be visually obvious.
- Keyboard must not cover the input.
- Send is disabled when no valid text exists.
- Send becomes active when text is entered.

---

## 17. Search Field

- Height: **52dp**
- Radius: **14dp**
- Leading search icon: 24dp
- Trailing clear icon: 20dp
- Placeholder: `Search medical resources...`

---

## 18. User Message

- Background: `#163A35`
- Radius: **16dp**
- Padding: **16dp**
- Text: Text Primary

User messages must be visually distinct from MEDIA answers without introducing an inconsistent color language.

---

## 19. MEDIA Answer Card

- Background: Surface
- Radius: **16dp**
- Padding: **20dp**

Structure:

```
Title

Answer

Category

Sources >
```

The card must prioritize readable medical content.

Avoid arbitrary empty space and unnecessary borders around every subsection.

---

## 20. Source Item

- Height: **52–64dp**
- Radius: **12dp**
- Background: Surface Variant

Structure:
- Icon
- Source title
- Source type
- Chevron

If an item is clickable, its interaction must be visually clear.

---

## 21. Resource Item

- Height: **72dp**
- Padding: **12dp**
- Radius: **12dp**

Structure:
- Resource name
- Subject
- Type / metadata
- Chevron

Example:

```
Muscle Physiology
Physiology • Theory
PDF • 12 pages
```

---

## 22. Chips

- Height: **32dp**
- Radius: **999dp**
- Horizontal padding: **12dp**

Examples:
- Physiology
- Pathology
- Anatomy
- Theory
- Practical
- PDF

---

## 23. Settings Row

- Height: **68–72dp**

Structure:
- Icon
- Title
- Description / Value
- Chevron or Switch

All settings screens must use the same row pattern.

---

## 24. Switch

- Width: **52dp**
- Height: **32dp**

ON: Primary  
OFF: Surface Variant

Use one consistent switch throughout the application.

---

## 25. Loading

Preferred patterns:
- Subtle progress indicator
- Skeleton
- Sending animation
- Progressive content appearance

Avoid using a plain loading message as the primary visual experience.

---

## 26. Empty State

Structure:

```
Icon / small illustration

Title

Short explanation

Optional action
```

Example:

> No conversations yet  
> Your medical questions and explanations will appear here.  
> [ Ask a question ]

Empty states must be informative but compact.

---

## 27. Error State

Structure:

```
Error icon

Title

Explanation

[ Retry ]
```

Example:

> Unable to load resource  
> Something went wrong while opening this resource.  
> [ Retry ]

Do not communicate errors using color alone.

---

## 28. Dialog

- Width: Screen width - 40dp
- Radius: **20dp**
- Padding: **24dp**

Structure:
- Title
- Description
- Secondary action
- Primary action

---

## 29. Bottom Sheet

- Top radius: **24dp**
- Padding: **24dp**

Handle:
- Width: **36dp**
- Height: **4dp**
- Radius: 999dp

---

## 30. Accessibility

Minimum interactive touch target:

**48 × 48dp**

The visible icon may remain 24dp while the touch target is 48dp.

The interface must support:
- Scalable text
- High contrast
- Screen readers/content descriptions
- Keyboard accessibility where applicable
- No color-only meaning
- Clear focus states
- Adequate touch targets

---

## 31. RTL / LTR

Arabic: **RTL**  
English: **LTR**

Mixed medical content must remain readable.

Navigation icons and directional elements must respect layout direction.

Use Android start/end semantics instead of hard-coded left/right positioning wherever appropriate.

---

## 32. Motion

Animation must be functional and subtle.

Recommended durations:
- Micro interaction: **150ms**
- Normal transition: **200ms**
- Large transition: **300ms**

Avoid:
- Bounce-heavy transitions
- Excessive zoom
- Decorative spinning
- Neon effects
- Long animations

Motion should communicate state and navigation, not distract from study.

---

## 33. Card Usage

A card should only exist when it represents a meaningful content group.

Do not put every UI element inside a card.

> Fewer cards, stronger hierarchy.

---

## 34. Illustration Rules

MEDIA is an educational application, not an illustration gallery.

Use illustrations only when they:
- Improve comprehension
- Explain medical concepts
- Clarify an empty state
- Support a meaningful onboarding moment

Do not add decorative doctors, DNA, stethoscopes, microscopes, etc. to every screen.

---

## 35. Home Design Foundation

```
HomeScreen
│
├── Top Identity
├── Offline Status
├── Greeting
├── Medical Question Input
├── Quick Actions
├── Recent Activity
└── Bottom Navigation
```

The exact visible components depend on the Home state.

---

## 36. Chat Design Foundation

```
ChatScreen
│
├── Top App Bar
│
├── Conversation
│   ├── User Message
│   ├── Processing State
│   ├── Answer Card
│   └── Source Items
│
└── Medical Question Input
```

---

## 37. Library Design Foundation

Hierarchy:

```
Year
 ↓
Semester
 ↓
Block
 ↓
Subject
 ↓
Theory / Practical
 ↓
Resources
```

Example:

```
Second Year
 ↓
MSK
 ↓
Physiology
 ↓
Theory
 ↓
Muscle Physiology
ANS
MCQ Bank
Summary
PDF
```

---

## 38. Design DO / DON'T

### DO

- Use consistent spacing.
- Use defined color tokens.
- Use one icon family.
- Preserve medical readability.
- Use clear hierarchy.
- Respect safe areas.
- Support RTL/LTR.
- Design all important states.
- Keep offline status calm and explicit.
- Make interactive elements obvious.
- Use real data only.
- Preserve existing functionality.

### DON'T

- Invent new colors without approval.
- Invent new component styles.
- Replace the design system per screen.
- Add fake data.
- Add unnecessary cards.
- Add decorative gradients.
- Add neon effects.
- Use random icons.
- Make text low contrast.
- Change AI behavior for visual reasons.
- Delete existing working functionality.
- Redesign the Knowledge Engine.
- Hide errors behind generic loading screens.

---

## 39. Android Implementation Rule

When implemented in the MEDIA Android project, the Design System is the source of truth.

Implementation should use centralized theme, tokens, and reusable components wherever the existing architecture allows.

Avoid hard-coding different colors, dimensions, typography, or component behavior separately in every screen.

Centralize:
- Colors
- Typography
- Spacing
- Radius
- Components
- Navigation
- States

---

## 40. AI Agent Rule

The following rule applies to OpenHands and any other implementation agent:

> **The approved MEDIA Design System and approved visual references are the visual source of truth. Do not redesign, reinterpret, replace, simplify, or invent UI patterns unless explicitly instructed.**

The implementation agent must:
1. Inspect the existing project.
2. Preserve working functionality.
3. Map existing functionality to the approved UI.
4. Implement the design system centrally.
5. Reuse components.
6. Implement all specified states.
7. Respect RTL/LTR.
8. Respect Android system insets.
9. Build the APK.
10. Test on a real/emulated device.
11. Capture screenshots.
12. Compare implementation against approved visual references.
13. Fix visual deviations.
14. Repeat until the visual result is consistent.

---

## 41. Protected Functional Layer

The following areas are outside the scope of a UI redesign unless explicitly requested:

- AI Model
- Knowledge Engine
- Knowledge Repository
- Retrieval
- Reasoning
- Verification
- Safe Abstention
- Medical Dataset
- Medical Records
- Existing Functional Data Flow

UI redesign must not silently modify these systems.

---

## 42. Versioning

Current version:

**MEDIA Design System v1.0**

Future changes should increment the version.

Examples:
- v1.1 — minor visual/component refinement
- v1.2 — additional approved components
- v2.0 — major visual-system change

A change to a global token must be documented before implementation.

---

## 43. Visual Source of Truth

The final visual source of truth consists of:

1. `MEDIA_DESIGN_SYSTEM_V1.md`
2. `MEDIA_UI_BLUEPRINT.md`
3. Approved visual mockups
4. Approved component sheets
5. Interaction specification

No individual screenshot should override the global design system unless explicitly marked as an approved exception.

---

## 44. Final Product Principle

MEDIA should feel like:

> **A professional medical education environment powered by AI.**

Not:

> A generic AI chatbot with medical colors.

The visual hierarchy must always communicate:

```
Medical Content
      ↓
Readable Explanation
      ↓
Useful Interaction
      ↓
Supporting Navigation
```

The interface exists to help the student understand, search, review, and study medical knowledge efficiently.
