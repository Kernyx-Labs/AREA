# Shared UI Components

Reusable Vue 3 components live in `web/src/components/ui`. Import what you need:

```js
import { Button, Input, Select, Card, Badge, Modal, Spinner } from '@/components/ui'
```

## Button
- Props: `variant` (`primary`|`secondary`|`ghost`|`danger`, default `primary`), `size` (`sm`|`md`|`lg`), `block` (bool), `loading` (bool), `disabled` (bool), `type` (`button`/`submit`/`reset`).
- Emits: `click` (suppressed while disabled/loading).
- Example:
```vue
<Button variant="primary" size="sm" :loading="saving" @click="save">Save</Button>
<Button variant="ghost" block>Full width</Button>
```

## Input
- Props: `modelValue`, `type`, `label`, `placeholder`, `hint`, `error`, `disabled`.
- Slots: `prefix`, `suffix` for inline adornments.
- Emits: `update:modelValue`.
- Example:
```vue
<Input v-model="email" label="Email" type="email" placeholder="you@example.com" hint="We never share your email" />
<Input v-model="search" placeholder="Search"> <template #prefix>🔍</template> </Input>
```

## Select
- Props: `modelValue`, `label`, `placeholder`, `hint`, `error`, `disabled`, `options: { value, label }[]`.
- Emits: `update:modelValue`.
- Example:
```vue
<Select v-model="plan" label="Plan" :options="[{ value: 'free', label: 'Free' }, { value: 'pro', label: 'Pro' }]" />
```

## Badge
- Props: `variant` (`neutral`|`success`|`danger`|`info`|`warning`|`accent`), `pill` (bool).
- Example:
```vue
<Badge variant="success" pill>Connected</Badge>
```

## Card
- Props: `hover` (bool).
- Slots: `header`, default, `footer`.
- Example:
```vue
<Card hover>
  <template #header>Profile</template>
  Content here
  <template #footer>
    <Button size="sm">Action</Button>
  </template>
</Card>
```

## Modal
- Props: `open` (bool), `backdropClosable` (bool, default true).
- Emits: `close`.
- Slots: `title`, default body, `footer`.
- Example:
```vue
<Modal :open="show" @close="show=false">
  <template #title>Confirm</template>
  Are you sure?
  <template #footer>
    <Button variant="ghost" @click="show=false">Cancel</Button>
    <Button variant="primary">Yes</Button>
  </template>
</Modal>
```

## Spinner
- Props: `size` (`xs`|`sm`|`md`|`lg`), `color` (CSS color, default `currentColor`).
- Example: `<Spinner size="sm" />`

## Integration example (Services view)
`web/src/components/ServicesView.vue` now uses `Button`, `Badge`, and `Spinner` to render actions and statuses, replacing ad-hoc button styles.

## Notes
- Components rely on palette tokens from `web/src/assets/styles.css` for theming and dark-mode support.
- Import via alias `@/components/ui` if your Vite alias maps `@` to `src` (default in this repo).

