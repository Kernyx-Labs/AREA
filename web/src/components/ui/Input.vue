<script setup>
-import { } from 'vue'
-const props = defineProps({
+import { computed, useSlots } from 'vue'
+const props = defineProps({
   modelValue: [String, Number],
   type: { type: String, default: 'text' },
   label: { type: String, default: '' },
   placeholder: { type: String, default: '' },
   hint: { type: String, default: '' },
   error: { type: String, default: '' },
   disabled: { type: Boolean, default: false }
 })
 const emit = defineEmits(['update:modelValue'])

-const controlClass = computed(() => ({ 'has-prefix': !!slots.prefix, 'has-suffix': !!slots.suffix, 'has-error': !!props.error }))
-
-const slots = useSlots()
+const slots = useSlots()
+const controlClass = computed(() => ({ 'has-prefix': !!slots.prefix, 'has-suffix': !!slots.suffix, 'has-error': !!props.error }))

 function onInput(event) {
   emit('update:modelValue', event.target.value)
 }
 </script>

