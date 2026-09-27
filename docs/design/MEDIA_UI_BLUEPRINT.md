# MEDIA UI Blueprint v1.0

**Product:** MEDIA — Medical Education AI  
**Platform:** Android Mobile  
**Status:** Approved structural blueprint / pre-visualization source of truth  
**Reference frame:** 390 × 844dp  
**Depends on:** MEDIA_DESIGN_SYSTEM_V1.md

---

## 1. Purpose

This document translates the MEDIA Design System into a complete screen-by-screen product UI specification.

It defines screens, navigation, states, layout hierarchy, components, labels, interactions, loading/error/offline/no-result behavior, visual-reference requirements, and implementation boundaries.

It does not define or modify AI reasoning, retrieval, verification, knowledge data, model behavior, or existing working data flows.

The implementation agent must treat this blueprint and approved visual mockups as the UI source of truth.

---

## 2. Global Navigation Model

Primary navigation:

Home
- Ask → Chat
- Quick action → Chat / Library / Search
- Recent item → Chat / Resource Detail

Library
- Year → Semester → Block → Subject → Theory/Practical → Resource Detail

History
- Conversation → Chat

More
- Profile
- Settings

Global destinations:
1. Home
2. Library
3. History
4. More

Chat is a task destination, not a bottom-navigation item.

---

## 3. Global UI Rules

Every screen must:
- respect system bars and keyboard insets
- use Android start/end rather than hard-coded left/right
- support RTL Arabic and LTR English
- use the centralized MEDIA theme/tokens
- use minimum 48 × 48dp touch targets
- preserve medical text readability
- distinguish Offline from Error
- provide explicit loading, empty, error, and disabled states where relevant
- avoid fake content

Global top app bar:
- 64dp
- 20dp horizontal padding
- 24dp navigation/action icons
- 20sp title

Global bottom navigation:
- Home / Library / History / More
- 24dp icon
- 12sp label
- approximately 80dp + system inset

---

## 4. Screen Inventory

| ID | Screen | Required |
|---|---|---|
| S01 | Splash | Yes |
| S02 | Home — First Launch | Yes |
| S03 | Home — Normal | Yes |
| S04 | Home — With Recent Activity | Yes |
| S05 | Home — Offline | Yes |
| S06 | Chat — Empty | Yes |
| S07 | Chat — Processing | Yes |
| S08 | Chat — Answer | Yes |
| S09 | Chat — Long Answer | Yes |
| S10 | Chat — Sources | Yes |
| S11 | Chat — Follow-up | Yes |
| S12 | Chat — No Result / Safe Abstention | Yes |
| S13 | Chat — Resource Error | Yes |
| S14 | Library — Root | Yes |
| S15 | Library — Year | Yes |
| S16 | Library — Semester / Block | Yes |
| S17 | Library — Subject | Yes |
| S18 | Library — Theory / Practical | Yes |
| S19 | Library — Resources | Yes |
| S20 | Resource Detail | Yes |
| S21 | Search — Empty | Yes |
| S22 | Search — Results | Yes |
| S23 | Search — No Results | Yes |
| S24 | History — Empty | Yes |
| S25 | History — Conversations | Yes |
| S26 | Profile | Yes |
| S27 | Settings | Yes |
| S28 | Settings — Appearance | Yes |
| S29 | Settings — Language | Yes |
| S30 | Settings — Notifications | Yes |
| S31 | Settings — Storage | Yes |
| S32 | Settings — App | Yes |
| S33 | Clear/Delete Confirmation | Yes |
| S34 | Generic Error / Recovery | Yes |

---

## 5. S01 — Splash

Purpose: short launch transition into Home.

Layout:
- Background
- MEDIA mark
- MEDIA
- Medical Education AI
- subtle loading indicator

Rules:
- Minimal.
- No marketing carousel.
- No fake progress percentage.
- Do not block unnecessarily while waiting for nonessential data.

States:
- Launching
- Ready
- Recovery/error

Navigation: Splash → Home.

---

## 6. S02 — Home / First Launch

Purpose: introduce the primary action immediately.

Layout:
- Top Identity
- Offline Status
- Greeting
- Short academic description
- Medical Question Input
- Quick Actions
- Optional Library prompt
- Bottom Navigation

Suggested copy:

Title:
> What are you studying today?

Supporting text:
> Ask a medical question or explore your local resources.

Input:
> Ask a medical question...

Quick actions:
- Ask a Question
- Browse Library
- Search Resources

Interaction:
- Tapping input → keyboard + focus.
- Entering text → send becomes active.
- Send → Chat.
- Browse Library → Library.
- Search Resources → Search.

---

## 7. S03 — Home / Normal

Layout:
- Top Identity
- Offline Status
- Greeting
- Medical Question Input
- Quick Actions
- Recent Activity
- Bottom Navigation

Recent activity appears only when actual history exists.

Do not show fabricated recent items.

---

## 8. S04 — Home / With Recent Activity

Recent activity item:
- Question / topic
- Short metadata
- Time
- Chevron

Tap → reopen the corresponding conversation.

Long press or overflow actions should only be added if an approved interaction exists.

---

## 9. S05 — Home / Offline

Offline is a normal capability state.

Show:
> Offline • Local resources available

Do not show an error banner if local functionality is available.

If an action requires unavailable external data, show the specific limitation only when triggered.

---

## 10. Chat Architecture

Chat is the central learning workflow.

Structure:
- Top App Bar
- Conversation Scroll Area
  - User Message
  - Processing
  - MEDIA Answer
  - Sources
  - Follow-up
- Medical Question Input

The composer must remain accessible above the keyboard.

---

## 11. S06 — Chat / Empty

Top bar:
- Back
- Title: New question
- Optional overflow only if needed

Body:
> Ask a medical question

> MEDIA will explain the answer using available local medical resources.

Composer:
> Ask a medical question...

No fake conversation content.

---

## 12. S07 — Chat / Processing

After Send:
1. User message appears immediately.
2. Composer enters sending/processing state.
3. Answer area shows subtle loading state.
4. Existing processing text, if used, must not dominate the screen.

Preferred visual:
- subtle progress indicator
- skeleton answer structure
- no giant spinner
- no flashing animation

---

## 13. S08 — Chat / Answer

Answer structure:
- Answer Title
- Category / subject metadata
- Definition / direct answer
- Explanation
- Key points where appropriate
- Sources
- Follow-up area

The existing MEDIA engine supplies medical content.

The UI must render:
- headings
- paragraphs
- lists
- medical terms
- Arabic explanation
- English terminology
- source references

Do not flatten structured content into one unreadable paragraph.

---

## 14. S09 — Chat / Long Answer

Rules:
- generous line height
- clear section hierarchy
- no nested card around every paragraph
- maintain scroll position
- sources remain reachable
- composer remains available

If content is very long, use progressive sections rather than artificial truncation.

Do not silently summarize or modify medical content for visual reasons.

---

## 15. S10 — Chat / Sources

Sources can be opened from the answer.

Each source item:
- source icon
- title
- type / metadata
- chevron if actionable

Tap → Resource Detail when a local resource exists.

If no registered source exists, do not invent one.

---

## 16. S11 — Chat / Follow-up

The user can ask another question from the same conversation.

Composer stays visually consistent with Home/Chat.

The conversation must retain context according to existing application behavior.

Do not alter AI context logic as part of UI implementation.

---

## 17. S12 — Chat / No Result / Safe Abstention

Critical safety state.

Structure:
- neutral status icon
- title
- clear explanation
- optional next action

Suggested UI copy:
> I couldn't find enough registered resources to answer this safely.

Optional:
> Try another question or browse the Library.

Do not fabricate medical information, sources, resources, or links.

The UI must not make abstention look like an application crash.

---

## 18. S13 — Chat / Resource Error

Use only for an actual loading/opening failure.

Copy:
> Unable to load resource

Supporting:
> Something went wrong while opening this resource.

Actions:
- Retry
- Back

Do not confuse this with Offline status or safe abstention.

---

## 19. S14 — Library / Root

Purpose: entry into structured medical resources.

Layout:
- Top App Bar
- Search
- Library hierarchy
- Bottom Navigation

Primary hierarchy:
Second Year → Semester → Block → Subject

Only display levels/data actually registered in the repository.

---

## 20. S15 — Library / Year

List:
- Year item
- metadata if available
- chevron

Example:
> Second Year

Tap → Semester.

---

## 21. S16 — Library / Semester / Block

Header:
- current hierarchy path
- title

Items:
- semester/block names
- metadata
- chevron

Use breadcrumb/context only when it improves navigation.

Back must always work.

---

## 22. S17 — Library / Subject

Example structure:
- Physiology
  - Theory
  - Practical
- Anatomy
  - Theory
  - Practical

Subject item should communicate subject and type when applicable.

Resource counts may appear only when real.

---

## 23. S18 — Library / Theory / Practical

Segmented choice:
- Theory
- Practical

Use a consistent selected/unselected state.

Do not create tabs when only one category exists; show the available category directly.

---

## 24. S19 — Library / Resources

Resource list:
- Resource name
- Subject / category
- Type / metadata
- Chevron

Real resource types may include:
- PDF
- Notes
- MCQ
- Lecture
- Practical

Do not invent metadata.

Search/filter controls may be shown when the number of resources makes them useful.

---

## 25. S20 — Resource Detail

Layout:
- Top App Bar
- Resource title
- Metadata chips
- Content
- Related resources

Content should prioritize reading.

Actions, when supported:
- Open
- Search within
- Ask MEDIA about this resource

Do not add actions unsupported by the existing application.

---

## 26. S21 — Search / Empty

Search field:
> Search medical resources...

Below:
- concise explanation
- optional recent searches only if real history exists

Search icon and input must have clear focus/readability.

---

## 27. S22 — Search / Results

Results use Resource Items.

Each result:
- title
- subject
- type
- relevant metadata
- chevron

Search must not display fake results while loading.

---

## 28. S23 — Search / No Results

Structure:
- Search field
- no-results icon
- No results found
- Try another medical term or browse the Library.

Keep the state compact.

---

## 29. S24 — History / Empty

Title:
> No conversations yet

Supporting:
> Your medical questions and explanations will appear here.

Action:
> Ask a question

Action → Chat.

---

## 30. S25 — History / Conversations

Conversation item:
- Question/topic
- Short preview
- Time/date
- Chevron

Only real conversation history is displayed.

Optional delete action must require confirmation.

---

## 31. S26 — Profile

Purpose: identity and app-level personal settings only.

Layout:
- Top App Bar
- Profile identity
- App preferences
- About

Do not introduce account functionality unless it exists in the current product.

---

## 32. S27 — Settings

Settings groups:

Appearance
- Theme / appearance

Language
- Interface language

Notifications
- Notification preferences

Storage
- Local data/cache information

App
- Version
- About
- Privacy/help items when actually implemented

Each group navigates to a dedicated screen where complexity warrants it.

---

## 33. S28 — Settings / Appearance

Rows:
- Theme
- System / Light / Dark if supported

The current MEDIA visual system is dark-first.

Do not promise unsupported light mode.

---

## 34. S29 — Settings / Language

Rows:
- Arabic
- English
- System default if supported

Language changes must respect:
- RTL/LTR
- typography
- mixed medical terminology

---

## 35. S30 — Settings / Notifications

Only show notification settings supported by the app.

States:
- enabled
- disabled

Use switches consistently.

---

## 36. S31 — Settings / Storage

Show actual local storage information when available.

Possible rows:
- Knowledge resources
- Cache
- Clear cache

Do not display invented storage sizes.

Destructive actions require confirmation.

---

## 37. S32 — Settings / App

Possible:
- App version
- About MEDIA
- Legal/help information if actually available

Keep it minimal.

---

## 38. S33 — Clear/Delete Confirmation

Dialog:

Title:
> Delete conversation?

Description:
> This conversation will be removed from your local history.

Actions:
- Cancel
- Delete

Destructive action uses semantic error styling.

Exact behavior must match the existing history implementation.

---

## 39. S34 — Generic Error / Recovery

Use only for unexpected UI/data loading failures.

Structure:
- error icon
- title
- short explanation
- Retry
- Back where appropriate

Never replace a specific safe-abstention or offline state with this generic error.

---

## 40. Shared Component Inventory

Navigation:
- BottomNavigation
- TopAppBar
- BackButton
- NavigationItem

Inputs:
- MedicalQuestionInput
- SearchField
- ClearButton
- SendButton

Content:
- AnswerCard
- AnswerSection
- UserMessageBubble
- SourceItem
- ResourceItem
- ConversationItem
- MetadataChip

Feedback:
- LoadingState
- SkeletonAnswer
- EmptyState
- ErrorState
- OfflineStatus
- SafeAbstentionState

Settings:
- SettingsSection
- SettingsRow
- Switch
- SelectionRow

Overlays:
- ConfirmDialog
- BottomSheet

No screen should create a visually unrelated one-off version of an existing component.

---

## 41. Component State Matrix

MedicalQuestionInput:
- Empty
- Focused empty
- Typing
- Sending
- Disabled
- Error
- RTL Arabic
- LTR English
- Mixed Arabic/English

Primary Button:
- Normal
- Pressed
- Disabled
- Loading

Resource Item:
- Normal
- Pressed
- Disabled if applicable
- Loading if applicable

Source Item:
- Normal
- Pressed
- Unavailable
- Loading if applicable

Offline Status:
- Online
- Offline with local resources
- Limited capability
- Actual error

Answer:
- Loading
- Answer
- Long answer
- Sources expanded
- Safe abstention
- Error

---

## 42. Navigation Rules

Home:
- Ask → Chat
- Library → Library
- Search → Search
- Recent conversation → Chat

Library:
- Year → Semester
- Semester → Block
- Block → Subject
- Subject → Theory/Practical
- Theory/Practical → Resources
- Resource → Resource Detail

History:
- Conversation → Chat

More:
- Profile → Profile
- Settings → Settings

Chat:
- Source → Resource Detail
- Back → previous screen
- Follow-up → same conversation

---

## 43. Back Behavior

Android Back must be predictable.

Priority:
1. Close keyboard/input focus where appropriate.
2. Close active dialog/bottom sheet.
3. Return from Resource Detail.
4. Return from nested Library level.
5. Leave Chat to previous screen.
6. Leave current root destination according to normal Android navigation behavior.

Do not implement custom back behavior that traps the user.

---

## 44. Keyboard Behavior

For Home and Chat:
- composer must move above IME
- conversation should remain readable
- focused input must remain visible
- sending must not hide the latest answer
- dismissing keyboard must restore full layout

The current white-on-white input problem is explicitly a release-blocking visual defect.

---

## 45. Arabic / English Rendering

Arabic explanation:
- Noto Sans Arabic
- RTL layout
- clear paragraph separation
- 16sp medical body
- 26sp line height

English medical terminology:
- Inter
- LTR rendering where appropriate

Mixed content:
- Unicode-aware bidirectional rendering
- never manually reverse strings
- avoid hard-coded alignment

Example:
Iron deficiency anemia
فقر الدم بنقص الحديد

Medical English terms remain intact inside Arabic explanation.

---

## 46. Visual Mockup Requirements

Before implementation, create approved visual references for at least:

Core:
1. Splash
2. Home First Launch
3. Home Normal
4. Home With Recent Activity
5. Home Offline
6. Chat Empty
7. Chat Processing
8. Chat Answer
9. Chat Long Answer
10. Chat Safe Abstention
11. Chat Error
12. Sources

Library:
13. Library Root
14. Library Nested
15. Resources
16. Resource Detail

Search:
17. Search Empty
18. Search Results
19. Search No Results

History:
20. History Empty
21. History List

Settings:
22. Settings
23. Appearance
24. Language
25. Storage
26. App

Components:
27. Input states sheet
28. Button states sheet
29. Answer/source/resource components
30. Empty/error/loading states

These images become the Visual Source of Truth only after approval.

---

## 47. Recommended Design Asset Repository Structure

Approved design references belong under:

docs/design/
- MEDIA_DESIGN_SYSTEM_V1.md
- MEDIA_UI_BLUEPRINT.md
- MEDIA_VISUAL_SOURCE_OF_TRUTH.md
- mockups/
  - splash/
  - home/
  - chat/
  - library/
  - search/
  - history/
  - profile/
  - settings/
- components/
  - buttons/
  - inputs/
  - cards/
  - navigation/
  - dialogs/
  - states/

This directory contains design references, not runtime Android resources.

---

## 48. Runtime Asset Rule

Assets actually used by Android belong in:

app/src/main/res/
- drawable/
- drawable-nodpi/
- mipmap-*/
- values/
- other Android resource directories as required

Rules:
- Vector icons → drawable XML where appropriate.
- Raster runtime images → drawable/drawable-nodpi as appropriate.
- Launcher icons → mipmap.
- Theme tokens → centralized values/theme implementation.
- Do not duplicate the same asset unnecessarily.

A mockup screenshot must never be copied into runtime resources merely because it visually contains a button or screen.

---

## 49. Button / Component Implementation Rule

Buttons and inputs should normally be native/reusable Android components, not screenshots.

Use images only when the visual itself is an actual asset:
- logo
- illustration
- educational diagram
- approved artwork

The visual design of a button is defined by the Design System; its implementation should remain responsive and accessible.

---

## 50. Visual Source of Truth Workflow

Design System
→ UI Blueprint
→ Visual Mockups
→ User Approval
→ MEDIA_VISUAL_SOURCE_OF_TRUTH.md
→ Android Implementation
→ APK
→ Device Screenshots
→ Visual QA
→ Fix deviations
→ Final APK

OpenHands must not begin a full UI rewrite before the visual source is approved.

---

## 51. Protected Functional Boundary

UI work must not modify:
- AI model
- model loading/inference
- Knowledge Engine
- Knowledge Repository
- retrieval
- reasoning
- verification
- safe abstention
- medical records
- medical dataset
- existing working data flows

If a UI change exposes a functional defect, report it separately unless fixing it is explicitly authorized.

---

## 52. Implementation Acceptance Criteria

A screen is considered implemented only when:
- Layout matches approved reference.
- Colors use design tokens.
- Typography matches the Design System.
- Spacing and radii match.
- RTL/LTR works.
- Touch targets are accessible.
- Keyboard behavior is correct.
- Loading state exists where needed.
- Empty state exists where needed.
- Error state exists where needed.
- Offline state is correctly distinguished.
- Real data is preserved.
- No fake data was added.
- Existing functionality still works.
- APK builds successfully.
- Screen is tested on an Android device/emulator.
- Screenshot comparison has been performed.

---

## 53. Release-Blocking Visual Defects

The following block visual acceptance:
1. White text on a white/light input background.
2. Invisible cursor.
3. Unreadable Arabic text.
4. Broken RTL.
5. Bottom navigation overlapping system navigation.
6. Keyboard covering the composer.
7. Missing loading state during processing.
8. Safe abstention presented as a crash/error.
9. Offline presented as an error when local resources work.
10. Random/unapproved colors.
11. Mixed icon styles.
12. Unreadable long medical answers.
13. Fake resource/source data.
14. Screen-specific redesign that violates the global Design System.

---

## 54. Final UI Principle

MEDIA should feel like:

> A professional medical education environment powered by AI.

The interface hierarchy remains:

Medical Content
→ Readable Explanation
→ Useful Interaction
→ Supporting Navigation

The UI exists to make medical learning clearer, faster, safer, and easier to navigate—not to compete with the medical content.
