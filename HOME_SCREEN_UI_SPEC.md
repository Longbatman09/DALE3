# DALE Home Screen - UI Reference Guide

## Screen Layout

```
╔═════════════════════════════════════════════════╗
║ ☰              DALE              [          ]  ║  56dp Top Bar
║ Color: #0f3460 (Deep Blue)                     ║
╠═════════════════════════════════════════════════╣
║                                                 ║
║  🟣 G   Group_Name                             ║  Group Indicator
║                                                 ║  Row (48dp)
╠═════════════════════════════════════════════════╣
║                                                 ║
║  ╔─────────────────────────────────────────╗   ║
║  ║ WhatsApp + Telegram           🔒 Locked║   ║  Card 1
║  ║ app.whatsapp + app.telegram              ║   ║  (Locked)
║  ╚─────────────────────────────────────────╝   ║
║                                                 ║
║  ╔─────────────────────────────────────────╗   ║
║  ║ YouTube + Instagram           🔓 Open  ║   ║  Card 2
║  ║ youtube + instagram.com                  ║   ║  (Unlocked)
║  ╚─────────────────────────────────────────╝   ║
║                                                 ║
║  ╔─────────────────────────────────────────╗   ║
║  ║ Email + Messages              🔒 Locked║   ║  Card 3
║  ║ gmail + messages.app                     ║   ║  (Locked)
║  ╚─────────────────────────────────────────╝   ║
║                                                 ║
║  ╔─────────────────────────────────────────╗   ║
║  ║ Banking Apps                  🔒 Locked║   ║  Card 4
║  ║ bank.app + secure.banking                ║   ║  (Locked)
║  ╚─────────────────────────────────────────╝   ║
║                                                 ║
║                                                 ║
║                                              ⊕  ║  FAB (Add)
║                                              │  ║
║                                             50dp │
╚═════════════════════════════════════════════════╝
```

## Color Palette

### Main Colors:
```
┌─────────────────────────────────────────┐
│ Color Name      │ Hex Code │ Usage      │
├─────────────────────────────────────────┤
│ Dark Navy       │ #1a1a2e  │ BG Gradient│
│ Darker Navy     │ #16213e  │ BG Gradient│
│ Deep Blue       │ #0f3460  │ Top Bar    │
│ Card Background │ #0f3460  │ Card BG    │
│ Purple40 (Accent)│ (defined)│ FAB, Avatar│
│ White           │ #FFFFFF  │ Text       │
│ Gray            │ #CCCCCC  │ Subtitle   │
│ Lock (Locked)   │ #4CAF50  │ Green      │
│ Lock (Open)     │ #757575  │ Gray       │
└─────────────────────────────────────────┘
```

## Component Dimensions

### Top Bar:
- Height: 56dp
- Padding: 16dp horizontal
- Menu Icon: 40dp (button) / 24dp (icon)
- Title Font: 24sp bold
- Background: #0f3460

### Group Indicator:
- Height: 48dp
- Avatar Circle: 32dp
- Padding: 16dp horizontal, 12dp vertical
- Avatar Text: 16sp bold
- Label Text: 14sp
- Background: Transparent

### Divider:
- Height: 1dp
- Padding: 16dp horizontal
- Color: #30475e (subtle)

### Group Card:
- Width: Match parent - 32dp margins
- Height: Wrap content (70-80dp)
- Padding: 12dp all sides
- Border Radius: 8dp
- Shadow: 4dp elevation
- Spacing: 8dp between cards

#### Card Content:
- Group Name: 16sp, bold, white
- Subtitle: 12sp, normal, gray
- Subtitle Padding Top: 4dp
- Lock Indicator: 32dp circle
- Lock Emoji: 16sp
- Content Padding: 12dp

### Floating Action Button:
- Size: 56dp (Material standard)
- Bottom Padding: 16dp
- Right Padding: 16dp
- Icon Size: 32dp
- Color: Purple40
- Elevation: 6dp

## Typography

### Font Family: Default Material Font (System)

### Font Sizes:
```
┌──────────────────────────┐
│ Element    │ Size │ Style │
├──────────────────────────┤
│ Title      │ 24sp │ Bold  │
│ Card Title │ 16sp │ SemBd │
│ Subtitle   │ 12sp │ Normal│
│ Header     │ 14sp │ Normal│
│ Avatar     │ 16sp │ Bold  │
│ Lock       │ 16sp │ Emoji │
└──────────────────────────┘
```

## Card States

### Locked Card (PIN Set):
```
╔─────────────────────────────────────────╗
║ WhatsApp + Telegram           🟢 Locked  ║
║ com.whatsapp + org.telegram.m            ║
╚─────────────────────────────────────────╝
     Text: White               Icon: 🔒
     Emoji Color: Green (#4CAF50)
```

### Unlocked Card (No PIN):
```
╔─────────────────────────────────────────╗
║ YouTube + Instagram           ⚫ Open    ║
║ com.google.youtube + instagram            ║
╚─────────────────────────────────────────╝
     Text: White               Icon: 🔓
     Emoji Color: Gray (#757575)
```

## Spacing Standards

### Horizontal:
- Screen Padding: 16dp
- Card Padding: 12dp
- Element Gap: 8dp
- Icon/Text Gap: 8dp

### Vertical:
- Bar Height: 56dp
- Row Height: 48dp
- Card Spacing: 8dp
- Divider Margin: 12dp top/bottom
- Bottom Padding: 16dp (FAB clearance: 72dp)

## Interactive Elements

### Clickable Areas:
1. **Menu Icon** (40x40dp) - Opens menu (future)
2. **Card Area** - Open group details (future)
3. **FAB** (56x56dp) - Create new group

### Feedback:
- Cards: Ripple effect on click
- FAB: Color change on press
- Menu: Standard icon button ripple

## Responsive Behavior

### Portrait Mode (Primary):
- Full width cards minus margins
- Centered FAB at bottom-right
- Vertical scrolling with LazyColumn

### Landscape Mode (Future):
- Consider 2-column layout
- Adjusted FAB position
- Horizontal scrolling optimization

## Accessibility

### Color Contrast:
- ✅ White text on #0f3460: High contrast (WCAG AA)
- ✅ Gray text on #16213e: Medium contrast
- ✅ Icons on purple: High contrast
- ✅ Lock status colors: Distinguishable

### Touch Targets:
- ✅ Menu button: 40dp (≥ 48dp recommended)
- ✅ Cards: Full width, 70dp+ height
- ✅ FAB: 56dp standard
- ✅ Avatar: 32dp

### Text:
- Clear font hierarchy
- Readable font sizes (min 12sp)
- Good line spacing
- Proper contrast ratios

## States & Transitions

### Empty State:
```
╔═════════════════════════════════════════╗
║ ☰              DALE                    ║
╠═════════════════════════════════════════╣
║                                         ║
║                                         ║
║     No groups created yet.              ║
║     Tap + to create one.                ║
║                                         ║
║                                         ║
║                                      ⊕  ║
╚═════════════════════════════════════════╝
```

### Loaded State:
- Shows N group cards
- Proper spacing between cards
- Smooth scrolling

### Loading State (Future):
- Shimmer effect possible
- Progressive loading
- Skeleton screens (optional)

## Animation Opportunities

### Current:
- Card ripple effect (built-in)
- FAB press animation (built-in)

### Future Enhancements:
- Card entrance animations
- Fade-in for list items
- Scale animation on FAB press
- Swipe to delete
- Reorder animations (drag & drop)

