<template>
  <AlertModal
    v-if="modalState.type === 'alert'"
    :open="modalState.open"
    :title="modalState.title"
    :message="modalState.message"
    :variant="modalState.variant"
    :confirmText="modalState.confirmText"
    @confirm="handleConfirm"
    @close="closeModal"
  />

  <ConfirmModal
    v-if="modalState.type === 'confirm'"
    :open="modalState.open"
    :title="modalState.title"
    :message="modalState.message"
    :variant="modalState.variant"
    :confirmText="modalState.confirmText"
    :cancelText="modalState.cancelText"
    @confirm="handleConfirm"
    @cancel="handleCancel"
    @close="closeModal"
  />

  <InputModal
    v-if="modalState.type === 'input'"
    :open="modalState.open"
    :title="modalState.title"
    :message="modalState.message"
    :label="modalState.label"
    :placeholder="modalState.placeholder"
    :hint="modalState.hint"
    :defaultValue="modalState.defaultValue"
    :inputType="modalState.inputType"
    :required="modalState.required"
    :confirmText="modalState.confirmText"
    :cancelText="modalState.cancelText"
    @confirm="handleConfirm"
    @cancel="handleCancel"
    @close="closeModal"
  />
</template>

<script setup>
import { useModal } from '../composables/useModal.js'
import AlertModal from './ui/AlertModal.vue'
import ConfirmModal from './ui/ConfirmModal.vue'
import InputModal from './ui/InputModal.vue'

const { modalState, handleConfirm, handleCancel, closeModal } = useModal()
</script>
