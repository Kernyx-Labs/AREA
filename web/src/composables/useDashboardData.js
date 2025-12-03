import { ref } from 'vue'

export function useDashboardData() {
  const cards = ref([
    { title: 'Active Automations', value: 12, icon: '⚙️', description: 'Currently running', trend: { value: '+2', prefix: '', suffix: ' vs yesterday', direction: 'up' } },
    { title: 'Total Actions', value: 348, icon: '✅', description: 'Actions executed today', trend: { value: '+15%', prefix: '', suffix: '', direction: 'up' } },
    { title: 'Failed Actions', value: 3, icon: '❌', description: 'Errors today', trend: { value: '-1', prefix: '', suffix: ' vs yesterday', direction: 'down' } },
    { title: 'Users Online', value: 27, icon: '🧑', description: 'Concurrent', trend: { value: '+3', prefix: '', suffix: '', direction: 'up' } },
    { title: 'Average Latency', value: '182ms', icon: '⏱️', description: 'Mean processing time' },
    { title: 'Webhooks Received', value: 89, icon: '📥', description: 'Today so far', trend: { value: '+6%', prefix: '', suffix: '', direction: 'up' } },
    { title: 'Email Notifications', value: 54, icon: '📧', description: 'Sent today' },
    { title: 'Server CPU', value: '47%', icon: '🖥️', description: 'Load average', trend: { value: '+5%', prefix: '', suffix: '', direction: 'up' } }
  ])

  // Simulate future refresh pattern
  function refresh() {
    // Placeholder: future fetch from API
  }

  return { cards, refresh }
}

