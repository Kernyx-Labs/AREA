# Quick Start Guide - New Workflow Builder

## Getting Started

### 1. Run the Development Server

```bash
cd /home/pandor/Delivery/AREA/web
npm install
npm run dev
```

The app should be running at `http://localhost:5173`

### 2. Navigate to Pipeline Editor

- Click "Create New Area" from the dashboard
- Or go directly to `/editor/new`

### 3. Create Your First Workflow

**Step 1: Add a Trigger**
1. Look at the left sidebar (Service Palette)
2. Expand "Gmail"
3. Under "Triggers", drag "New Email Received"
4. Drop it onto the "Trigger Zone" at the top of the canvas

**Step 2: Configure the Trigger**
1. Double-click the trigger card
2. Fill in optional filters (or leave blank)
3. Click "Save"

**Step 3: Add an Action**
1. Expand "Discord" in the sidebar
2. Under "Actions", drag "Send Message"
3. Drop it onto the "Actions Zone" below the trigger

**Step 4: Configure the Action**
1. Double-click the action card
2. Select your connected Discord bot
3. Enter the channel ID where you want messages sent
4. Write a message template (use `{{sender}}`, `{{subject}}`, etc.)
5. Click "Save"

**Step 5: Save the Workflow**
1. Enter a workflow name at the top
2. Click "Save & Activate"
3. Done!

---

## Component Usage Examples

### Example 1: Using ServicePalette Standalone

```vue
<template>
  <div class="my-app">
    <ServicePalette
      @drag-start="handleDragStart"
      @drag-end="handleDragEnd"
    />
  </div>
</template>

<script setup>
import { ServicePalette } from '@/components/workflow'

function handleDragStart(data) {
  console.log('Dragging:', data.serviceName, data.eventName)
}

function handleDragEnd() {
  console.log('Drag ended')
}
</script>
```

### Example 2: Using WorkflowCanvas with Initial Data

```vue
<template>
  <WorkflowCanvas
    ref="canvasRef"
    :initial-trigger="initialTrigger"
    :initial-actions="initialActions"
    @workflow-change="handleChange"
  />
</template>

<script setup>
import { ref } from 'vue'
import { WorkflowCanvas } from '@/components/workflow'
import { MailIcon } from 'lucide-vue-next'

const canvasRef = ref(null)

const initialTrigger = {
  serviceName: 'Gmail',
  serviceColor: '#EA4335',
  serviceIcon: MailIcon,
  eventName: 'New Email Received',
  eventType: 'trigger',
  config: { senderFilter: 'boss@company.com' },
  configFields: []
}

const initialActions = []

function handleChange(workflow) {
  console.log('Workflow changed:', workflow)
}

// Access canvas methods
function getWorkflow() {
  return canvasRef.value?.getWorkflow()
}

function reset() {
  canvasRef.value?.reset()
}
</script>
```

### Example 3: Using WorkflowBlock Standalone

```vue
<template>
  <WorkflowBlock
    service-name="Gmail"
    service-color="#EA4335"
    :service-icon="MailIcon"
    event-name="New Email Received"
    event-description="Triggers when a new email arrives"
    event-type="trigger"
    :config-fields="configFields"
    :config="config"
    :show-remove="true"
    @remove="handleRemove"
    @save="handleSave"
  />
</template>

<script setup>
import { ref } from 'vue'
import { WorkflowBlock } from '@/components/workflow'
import { MailIcon } from 'lucide-vue-next'

const config = ref({
  senderFilter: '',
  subjectFilter: ''
})

const configFields = [
  {
    name: 'senderFilter',
    label: 'Filter by Sender',
    type: 'email',
    placeholder: 'sender@example.com',
    required: false
  },
  {
    name: 'subjectFilter',
    label: 'Filter by Subject',
    type: 'text',
    placeholder: 'Keywords...',
    required: false
  }
]

function handleRemove() {
  console.log('Remove clicked')
}

function handleSave(newConfig) {
  config.value = newConfig
  console.log('Saved:', newConfig)
}
</script>
```

---

## Testing the Components

### Manual Testing Checklist

**ServicePalette**
- [ ] Services load from API
- [ ] Expanding/collapsing works
- [ ] Search filters correctly
- [ ] Drag starts with correct data
- [ ] Icons and colors display

**WorkflowCanvas**
- [ ] Trigger zone accepts only triggers
- [ ] Action zone accepts only actions
- [ ] Visual feedback on drag over
- [ ] Error state on invalid drop
- [ ] Arrow connectors appear
- [ ] Add action button works
- [ ] Empty states show

**WorkflowBlock**
- [ ] Compact view displays correctly
- [ ] Double-click expands
- [ ] Form fields render
- [ ] Variable tags display
- [ ] Save updates config
- [ ] Cancel reverts changes
- [ ] Remove button works

**PipelineEditor**
- [ ] Name input works
- [ ] Test button validates
- [ ] Save button works
- [ ] Status messages appear
- [ ] Redirects after save
- [ ] Loads existing workflow

---

## Common Issues & Solutions

### Issue: Services Not Loading

**Problem**: Sidebar shows "Loading services..." forever

**Solutions**:
1. Check if backend is running on port 8080
2. Verify `/about.json` endpoint returns data
3. Check browser console for CORS errors
4. Ensure API_URL is configured in `.env`

### Issue: Drag and Drop Not Working

**Problem**: Items don't drag from sidebar

**Solutions**:
1. Ensure `draggable="true"` on event items
2. Check browser console for JavaScript errors
3. Verify drag event handlers are attached
4. Try a different browser

### Issue: Workflow Not Saving

**Problem**: Click "Save" but nothing happens

**Solutions**:
1. Check if trigger is added (required)
2. Verify workflow name is not empty
3. Open browser DevTools → Network tab
4. Look for API errors in console
5. Ensure service connections exist (for Gmail/Discord)

### Issue: Cards Not Expanding

**Problem**: Double-click doesn't expand card

**Solutions**:
1. Check if `@dblclick` handler is attached
2. Verify card is not in dragging state
3. Try single click then double-click
4. Check browser console for errors

---

## Keyboard Shortcuts

- **Tab**: Navigate through form fields
- **Enter**: Submit form (when in input field)
- **Escape**: Close expanded card (future)
- **Double-click**: Expand/collapse card
- **Ctrl+S**: Save workflow (future)

---

## API Endpoints Reference

### GET /about.json
Returns available services, triggers, and actions.

**Response**:
```json
{
  "server": {
    "services": [
      {
        "name": "Gmail",
        "actions": [...],    // Triggers
        "reactions": [...]   // Actions
      }
    ]
  }
}
```

### POST /api/workflows
Creates a new workflow.

**Request**:
```json
{
  "name": "My Workflow",
  "trigger": {
    "service": "Gmail",
    "event": "New Email Received",
    "config": {}
  },
  "actions": [
    {
      "service": "Discord",
      "event": "Send Message",
      "config": {}
    }
  ]
}
```

### GET /api/workflows/:id
Retrieves a workflow by ID.

**Response**:
```json
{
  "success": true,
  "data": {
    "workflow": {
      "id": 1,
      "name": "My Workflow",
      "active": true,
      "trigger": {...},
      "actions": [...]
    }
  }
}
```

---

## Development Tips

### Hot Module Replacement
Vite supports HMR - changes to Vue files will auto-reload without full page refresh.

### Vue DevTools
Install Vue DevTools browser extension for debugging:
- Inspect component hierarchy
- View component props and state
- Track events
- Profile performance

### CSS Variables
All colors use CSS custom properties. Edit `/web/src/assets/styles.css` to change theme.

### Icon Library
Using Lucide Vue. Import icons:
```javascript
import { MailIcon, GithubIcon } from 'lucide-vue-next'
```

---

## Next Steps

1. **Test the Interface**: Create a few workflows to ensure everything works
2. **Customize Styling**: Adjust colors, spacing, or animations to match your brand
3. **Add Services**: Extend the backend to support more services
4. **Implement Features**: Add action reordering, templates, validation, etc.
5. **Deploy**: Build and deploy to production

---

## Resources

- **Vue 3 Docs**: https://vuejs.org/
- **Vite Docs**: https://vitejs.dev/
- **Lucide Icons**: https://lucide.dev/
- **HTML5 Drag & Drop**: https://developer.mozilla.org/en-US/docs/Web/API/HTML_Drag_and_Drop_API

---

## Support

For issues or questions:
1. Check `/web/src/components/workflow/README.md`
2. Review `/REFACTOR_SUMMARY.md`
3. Check browser console for errors
4. Inspect network requests in DevTools
5. Create an issue in the repository

---

Happy building! 🚀
