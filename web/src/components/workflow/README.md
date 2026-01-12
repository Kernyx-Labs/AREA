# Workflow Builder Components

This directory contains the refactored drag-and-drop workflow builder components for the AREA Pipeline Editor.

## Architecture Overview

The workflow builder follows a modular component architecture with three main components:

1. **ServicePalette** - Left sidebar showing available services and their events
2. **WorkflowCanvas** - Main canvas with trigger and action zones
3. **WorkflowBlock** - Reusable card component for workflow steps

## Components

### ServicePalette.vue

**Purpose**: Displays all available services with their triggers and actions in a collapsible sidebar.

**Features**:
- Fetches services from `/about.json` API endpoint
- Displays services in expandable sections
- Shows triggers and actions separately
- Drag-and-drop enabled for all events
- Search functionality to filter services
- Visual icons and colors for each service

**Props**: None

**Events**:
- `drag-start` - Emitted when dragging starts with event data
- `drag-end` - Emitted when dragging ends

**Usage**:
```vue
<ServicePalette
  @drag-start="handleDragStart"
  @drag-end="handleDragEnd"
/>
```

---

### WorkflowCanvas.vue

**Purpose**: Main drop zone canvas with dedicated trigger slot and action list.

**Features**:
- **Trigger Zone**: Single drop zone at the top (only one trigger allowed)
- **Actions Zone**: List below trigger for multiple chained actions
- Visual arrow connectors between steps (SVG with dashed lines)
- Drop validation (only triggers in trigger zone, only actions in action zone)
- Visual feedback during drag (highlight, error states)
- Add action button with drop zone
- Empty states with helpful instructions

**Props**:
- `initialTrigger` (Object) - Pre-populate trigger
- `initialActions` (Array) - Pre-populate actions

**Events**:
- `workflow-change` - Emitted when workflow changes with `{ trigger, actions }`
- `trigger-change` - Emitted when trigger changes
- `actions-change` - Emitted when actions change

**Exposed Methods**:
- `getTrigger()` - Returns current trigger
- `getActions()` - Returns current actions array
- `getWorkflow()` - Returns complete workflow object
- `reset()` - Clears trigger and actions

**Usage**:
```vue
<WorkflowCanvas
  ref="canvasRef"
  :initial-trigger="trigger"
  :initial-actions="actions"
  @workflow-change="handleWorkflowChange"
/>
```

---

### WorkflowBlock.vue

**Purpose**: Reusable card component that displays workflow steps (triggers/actions).

**Features**:
- **Compact View**: Shows service icon + service name + event name
- **Expanded View**: Shows configuration form with all fields
- Double-click to toggle between compact and expanded
- Draggable in compact mode
- Configuration form with dynamic fields (text, textarea, select, number, etc.)
- Available variables display (for actions)
- Variable insertion helper
- Save/Cancel buttons
- Remove button

**Props**:
- `serviceName` (String, required) - Service name (e.g., "Gmail")
- `serviceColor` (String) - Service brand color
- `serviceIcon` (Component) - Lucide Vue icon component
- `eventName` (String, required) - Event name (e.g., "New Email")
- `eventDescription` (String) - Event description
- `eventType` (String, required) - "trigger" or "action"
- `configFields` (Array) - Field definitions for configuration form
- `config` (Object) - Current configuration values
- `availableVariables` (Array) - Variable names from trigger
- `showRemove` (Boolean) - Show remove button
- `expanded` (Boolean) - Initial expanded state
- `dragData` (Object) - Data to transfer on drag

**Events**:
- `remove` - Emitted when remove button clicked
- `save` - Emitted when save button clicked with config data
- `cancel` - Emitted when cancel button clicked
- `expand` - Emitted when card expands
- `collapse` - Emitted when card collapses

**Config Field Schema**:
```javascript
{
  name: 'fieldName',        // Field identifier
  label: 'Field Label',     // Display label
  type: 'text',             // text, email, url, number, textarea, select
  placeholder: 'Enter...',  // Placeholder text
  required: true,           // Is field required
  hint: 'Helper text',      // Optional hint text
  rows: 4,                  // For textarea
  options: [                // For select
    { value: 'val1', label: 'Label 1' },
    { value: 'val2', label: 'Label 2' }
  ]
}
```

**Usage**:
```vue
<WorkflowBlock
  service-name="Gmail"
  service-color="#EA4335"
  :service-icon="MailIcon"
  event-name="New Email Received"
  event-description="Triggers when a new email arrives"
  event-type="trigger"
  :config-fields="fields"
  :config="config"
  :show-remove="true"
  @remove="handleRemove"
  @save="handleSave"
/>
```

---

## Data Flow

### 1. Loading Services

```
ServicePalette
  └─> api.getAvailableServices()
      └─> GET /about.json
          └─> Returns: { server: { services: [...] } }
```

### 2. Dragging from Palette

```
User drags event from ServicePalette
  └─> ServicePalette emits 'drag-start' with event data
      └─> PipelineEditor receives event (optional handling)
          └─> WorkflowCanvas receives dragover event
              └─> Validates event type (trigger vs action)
                  └─> Shows visual feedback
```

### 3. Dropping on Canvas

```
User drops event on WorkflowCanvas
  └─> WorkflowCanvas validates drop zone
      └─> Creates WorkflowBlock with event data
          └─> Emits 'workflow-change' to PipelineEditor
              └─> PipelineEditor updates local state
```

### 4. Configuring Block

```
User double-clicks WorkflowBlock
  └─> Block expands, shows configuration form
      └─> User fills in fields
          └─> User clicks "Save"
              └─> Block emits 'save' with config data
                  └─> WorkflowCanvas updates block config
                      └─> Emits 'workflow-change' to PipelineEditor
```

### 5. Saving Workflow

```
User clicks "Save & Activate" in PipelineEditor
  └─> PipelineEditor builds workflow data
      └─> Checks if Gmail → Discord (use AREA API)
          └─> api.createArea(data) OR api.createWorkflow(data)
              └─> Workflow saved to backend
                  └─> Redirect to dashboard
```

---

## Styling

All components use the existing dark theme CSS variables:

- `--color-surface` - Card backgrounds
- `--color-border-default` - Default borders
- `--color-border-bright` - Highlighted borders
- `--color-accent-blue` - Primary accent color
- `--color-accent-purple` - Secondary accent
- `--color-text-primary` - Main text
- `--color-text-secondary` - Secondary text
- `--color-text-muted` - Muted text
- `--shadow-sm/md/lg` - Shadow levels
- `--radius-sm/md/lg` - Border radius
- `--space-xs/sm/md/lg/xl` - Spacing scale

---

## Drag & Drop Implementation

Uses native HTML5 Drag and Drop API:

**Dragging**:
```javascript
// On drag start
event.dataTransfer.setData('application/json', JSON.stringify(data))
event.dataTransfer.effectAllowed = 'copy'
```

**Dropping**:
```javascript
// On drop
const data = JSON.parse(event.dataTransfer.getData('application/json'))
// Validate and process
```

**Validation**:
- Trigger zone only accepts `eventType === 'trigger'`
- Action zone only accepts `eventType === 'action'`
- Visual error state when hovering over invalid zone

---

## Animation & Transitions

### Expand/Collapse
- WorkflowBlock smoothly transitions between compact and expanded states
- Height, padding, and opacity animate with CSS transitions

### Drag Feedback
- Dragged item becomes semi-transparent (`opacity: 0.5`)
- Drop zones highlight with blue border and background
- Invalid drop zones show red border

### Action List
- Vue's `<TransitionGroup>` for smooth list animations
- Items fade in/out and slide when added/removed
- Reordering animates position changes

### Arrow Connectors
- SVG lines with dashed stroke (`stroke-dasharray`)
- Arrowhead marker at the end
- Fade in when trigger is added

---

## Accessibility

- Semantic HTML structure
- ARIA labels where appropriate
- Keyboard navigation support (tab through form fields)
- Focus visible states
- High contrast colors (WCAG AA compliant)
- Clear visual feedback for all interactions

---

## Future Enhancements

Potential improvements for future iterations:

1. **Action Reordering**: Drag to reorder actions in the list
2. **Branching Logic**: Add conditional branching (if/then)
3. **Loops**: Repeat actions multiple times
4. **Templates**: Save and load workflow templates
5. **Undo/Redo**: Action history with undo/redo
6. **Validation**: Real-time validation of config fields
7. **Preview**: Preview workflow execution flow
8. **Export/Import**: Export as JSON, import from file
9. **Collaboration**: Real-time collaborative editing
10. **Mobile Touch**: Better touch support for mobile devices

---

## Troubleshooting

### Services not loading
- Check `/about.json` endpoint is accessible
- Verify API response format matches expected structure
- Check browser console for errors

### Drag and drop not working
- Ensure `draggable="true"` on draggable elements
- Check `dragstart`, `dragover`, `drop` event handlers
- Verify `event.preventDefault()` in `dragover` handler

### Config not saving
- Check `save` event is being emitted correctly
- Verify parent component is handling `save` event
- Check config data structure matches expected format

### Workflow not saving to backend
- Verify API endpoints are correct
- Check service connections exist (for Gmail/Discord)
- Verify required config fields are filled
- Check browser console and network tab for errors

---

## Examples

### Creating a Simple Gmail → Discord Workflow

1. Open Pipeline Editor (click "Create New Area")
2. Drag "Gmail - New Email Received" from palette to Trigger zone
3. Double-click the trigger block to configure
4. Fill in sender/subject filters (optional)
5. Click "Save" to collapse
6. Drag "Discord - Send Message" from palette to Actions zone
7. Double-click the action block to configure
8. Enter Discord webhook URL
9. Write message template using variables like `{{sender}}`
10. Click "Save" to collapse
11. Enter workflow name at top
12. Click "Save & Activate"

---

## API Integration

### Required API Endpoints

**GET /about.json**
```json
{
  "server": {
    "services": [
      {
        "name": "Gmail",
        "actions": [
          {
            "name": "New Email Received",
            "description": "Triggers when a new email arrives",
            "configFields": [
              {
                "name": "senderFilter",
                "label": "Filter by Sender",
                "type": "email",
                "required": false,
                "placeholder": "sender@example.com"
              }
            ]
          }
        ],
        "reactions": []
      }
    ]
  }
}
```

**POST /api/workflows**
```json
{
  "name": "My Workflow",
  "trigger": {
    "service": "Gmail",
    "event": "New Email Received",
    "config": { "senderFilter": "boss@company.com" }
  },
  "actions": [
    {
      "service": "Discord",
      "event": "Send Message",
      "config": { "webhookUrl": "...", "message": "..." }
    }
  ]
}
```

**POST /api/areas** (Legacy Gmail → Discord)
```json
{
  "actionConnectionId": 123,
  "reactionConnectionId": 456,
  "gmailLabel": "INBOX",
  "gmailSubjectContains": "",
  "gmailFromAddress": "",
  "discordWebhookUrl": "...",
  "discordChannelName": "general",
  "discordMessageTemplate": "New email: {{subject}}"
}
```

---

## Component File Structure

```
web/src/components/workflow/
├── README.md               # This file
├── ServicePalette.vue      # Left sidebar with services
├── WorkflowCanvas.vue      # Main canvas with trigger/action zones
└── WorkflowBlock.vue       # Reusable block/card component
```

---

## Testing Checklist

- [ ] Services load correctly from API
- [ ] Search filters services properly
- [ ] Drag from palette works
- [ ] Drop validation works (trigger vs action zones)
- [ ] Visual feedback during drag (highlight, errors)
- [ ] Double-click expands block
- [ ] Configuration form displays correctly
- [ ] All field types work (text, textarea, select, number)
- [ ] Save updates config
- [ ] Cancel reverts changes
- [ ] Remove deletes block
- [ ] Available variables display
- [ ] Variable insertion works
- [ ] Arrow connectors appear
- [ ] Multiple actions can be added
- [ ] Workflow saves to backend
- [ ] Error messages display properly
- [ ] Responsive layout works on mobile
- [ ] Keyboard navigation works
- [ ] Theme styling is consistent
