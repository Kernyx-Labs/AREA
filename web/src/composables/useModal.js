import { ref } from 'vue'

const modalState = ref({
  type: null, // 'alert' | 'confirm' | 'input'
  open: false,
  title: '',
  message: '',
  variant: 'info',
  confirmText: 'OK',
  cancelText: 'Cancel',
  inputType: 'text',
  placeholder: '',
  label: '',
  hint: '',
  defaultValue: '',
  required: true,
  resolve: null,
  reject: null
})

export function useModal() {
  /**
   * Show an alert modal (replaces window.alert)
   * @param {string} message - The message to display
   * @param {Object} options - Optional configuration
   * @returns {Promise<void>}
   */
  function alert(message, options = {}) {
    return new Promise((resolve) => {
      modalState.value = {
        ...modalState.value,
        type: 'alert',
        open: true,
        message,
        title: options.title || '',
        variant: options.variant || 'info',
        confirmText: options.confirmText || 'OK',
        resolve,
        reject: null
      }
    })
  }

  /**
   * Show a confirmation modal (replaces window.confirm)
   * @param {string} message - The message to display
   * @param {Object} options - Optional configuration
   * @returns {Promise<boolean>}
   */
  function confirm(message, options = {}) {
    return new Promise((resolve) => {
      modalState.value = {
        ...modalState.value,
        type: 'confirm',
        open: true,
        message,
        title: options.title || '',
        variant: options.variant || 'info',
        confirmText: options.confirmText || 'Confirm',
        cancelText: options.cancelText || 'Cancel',
        resolve,
        reject: null
      }
    })
  }

  /**
   * Show an input modal (replaces window.prompt)
   * @param {string} message - The message to display
   * @param {Object} options - Optional configuration
   * @returns {Promise<string|null>}
   */
  function prompt(message, options = {}) {
    return new Promise((resolve) => {
      modalState.value = {
        ...modalState.value,
        type: 'input',
        open: true,
        message,
        title: options.title || 'Input',
        label: options.label || '',
        placeholder: options.placeholder || '',
        hint: options.hint || '',
        defaultValue: options.defaultValue || '',
        inputType: options.inputType || 'text',
        required: options.required !== undefined ? options.required : true,
        confirmText: options.confirmText || 'Submit',
        cancelText: options.cancelText || 'Cancel',
        resolve,
        reject: null
      }
    })
  }

  /**
   * Handle modal confirmation
   * @param {any} value - The value to resolve with
   */
  function handleConfirm(value) {
    if (modalState.value.resolve) {
      if (modalState.value.type === 'alert') {
        modalState.value.resolve()
      } else if (modalState.value.type === 'confirm') {
        modalState.value.resolve(true)
      } else if (modalState.value.type === 'input') {
        modalState.value.resolve(value)
      }
    }
    closeModal()
  }

  /**
   * Handle modal cancellation
   */
  function handleCancel() {
    if (modalState.value.resolve) {
      if (modalState.value.type === 'confirm') {
        modalState.value.resolve(false)
      } else if (modalState.value.type === 'input') {
        modalState.value.resolve(null)
      }
    }
    closeModal()
  }

  /**
   * Close the modal
   */
  function closeModal() {
    modalState.value.open = false
    // Reset state after animation completes
    setTimeout(() => {
      modalState.value = {
        type: null,
        open: false,
        title: '',
        message: '',
        variant: 'info',
        confirmText: 'OK',
        cancelText: 'Cancel',
        inputType: 'text',
        placeholder: '',
        label: '',
        hint: '',
        defaultValue: '',
        required: true,
        resolve: null,
        reject: null
      }
    }, 200)
  }

  return {
    modalState,
    alert,
    confirm,
    prompt,
    handleConfirm,
    handleCancel,
    closeModal
  }
}
