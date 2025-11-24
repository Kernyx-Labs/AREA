<template>
  <div class="editor-root">
    <header class="editor-header">
      <input
        v-model="areaName"
        class="title-input"
        :style="{ borderColor: titleFocused ? 'rgba(255,255,255,0.5)' : 'transparent' }"
        @focus="titleFocused = true"
        @blur="titleFocused = false"
      />
      <div class="header-actions">
        <button class="mini-btn action" @click="openPicker('action', 150, 200)"><PlusIcon size="16" /> Action</button>
        <button class="mini-btn reaction" @click="openPicker('reaction', 450, 200)"><PlusIcon size="16" /> Reaction</button>
        <button class="save-btn"><PlayIcon size="16" /> Save & Activate</button>
      </div>
    </header>
    <div ref="canvasRef" class="canvas" @click="handleCanvasClick" @dblclick="handleCanvasDouble">
      <GridBackground />
      <ConnectionLine
        v-for="(conn,i) in connections"
        :key="i"
        :from="conn.from"
        :to="conn.to"
        :nodes="nodes"
      />
      <Node
        v-for="n in nodes"
        :key="n.id"
        :node="n"
        :isSelected="selectedNode === n.id"
        :connectingFrom="connectingFrom"
        @dragNode="handleDrag"
        @deleteNode="deleteNode"
        @selectNode="selectNode"
        @startConnect="startConnect"
        @endConnect="endConnect"
      />
      <ServicePicker
        v-if="picker"
        :type="picker.type"
        :position="{ x: picker.x, y: picker.y }"
        @close="picker=null"
        @select="addNode"
      />
      <div class="help">Double-click to add node • Drag nodes to move • Click connectors to link</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { PlusIcon, PlayIcon, XIcon } from 'lucide-vue-next'

// Data lists
const services = {
  gmail: { name: 'Gmail', color: '#EA4335' },
  timer: { name: 'Timer', color: '#4285F4' },
  github: { name: 'GitHub', color: '#6e40c9' },
  discord: { name: 'Discord', color: '#5865F2' },
  dropbox: { name: 'Dropbox', color: '#0061FF' },
  outlook: { name: 'Outlook', color: '#0078D4' }
}
const actionsList = [
  { service: 'gmail', name: 'New email received', desc: 'Triggers when a new email arrives' },
  { service: 'gmail', name: 'Email with attachment', desc: 'Triggers on emails with files' },
  { service: 'timer', name: 'Schedule', desc: 'Triggers at specific times' },
  { service: 'timer', name: 'Interval', desc: 'Triggers every X minutes' },
  { service: 'github', name: 'New Issue', desc: 'Triggers on new issues' },
  { service: 'github', name: 'Pull Request', desc: 'Triggers on new PRs' }
]
const reactionsList = [
  { service: 'discord', name: 'Send Message', desc: 'Posts a message to a channel' },
  { service: 'gmail', name: 'Send Email', desc: 'Sends an email' },
  { service: 'dropbox', name: 'Upload File', desc: 'Uploads a file to Dropbox' },
  { service: 'outlook', name: 'Create Event', desc: 'Creates a calendar event' },
  { service: 'github', name: 'Create Issue', desc: 'Creates a new issue' }
]

const areaName = ref('New Automation')
const titleFocused = ref(false)
const nodes = ref([
  { id: 1, type: 'action', service: 'github', actionName: 'New Issue', desc: 'Triggers on new issues', x: 100, y: 150 },
  { id: 2, type: 'reaction', service: 'discord', actionName: 'Send Message', desc: 'Posts to #alerts', x: 450, y: 150 }
])
const connections = ref([{ from: 1, to: 2 }])
const selectedNode = ref(null)
const picker = ref(null)
const connectingFrom = ref(null)
const canvasRef = ref(null)

function handleCanvasClick(e) {
  if (e.target === canvasRef.value || e.target.tagName === 'svg') {
    selectedNode.value = null
    connectingFrom.value = null
  }
}
function handleCanvasDouble(e) {
  if (e.target !== canvasRef.value && e.target.tagName !== 'svg') return
  const rect = canvasRef.value.getBoundingClientRect()
  picker.value = { x: e.clientX - rect.left, y: e.clientY - rect.top, type: 'action' }
}
function openPicker(type, x, y) { picker.value = { type, x, y } }
function addNode(item) {
  nodes.value.push({ id: Date.now(), type: picker.value.type, service: item.service, actionName: item.name, desc: item.desc, x: picker.value.x, y: picker.value.y })
  picker.value = null
}
function deleteNode(id) {
  nodes.value = nodes.value.filter(n => n.id !== id)
  connections.value = connections.value.filter(c => c.from !== id && c.to !== id)
}
function selectNode(id) { selectedNode.value = id }
function startConnect(id) { connectingFrom.value = id }
function endConnect(id) {
  if (connectingFrom.value && connectingFrom.value !== id) {
    const exists = connections.value.some(c => c.from === connectingFrom.value && c.to === id)
    if (!exists) connections.value.push({ from: connectingFrom.value, to: id })
  }
  connectingFrom.value = null
}
function handleDrag(id, startEvent) {
  const startX = startEvent.clientX
  const startY = startEvent.clientY
  const node = nodes.value.find(n => n.id === id)
  const originX = node.x
  const originY = node.y
  function onMove(ev) {
    const dx = ev.clientX - startX
    const dy = ev.clientY - startY
    node.x = originX + dx
    node.y = originY + dy
  }
  function onUp() {
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

// Sub components inline
const Node = {
  props: { node: Object, isSelected: Boolean, connectingFrom: Number },
  emits: ['dragNode','deleteNode','selectNode','startConnect','endConnect'],
  setup(props, { emit }) {
    function onMouseDown(e) {
      if (e.target.closest('.no-drag')) return
      e.stopPropagation()
      emit('dragNode', props.node.id, e)
    }
    function onSelect(e) { e.stopPropagation(); emit('selectNode', props.node.id) }
    function connector(type) {
      if (type === 'output' && props.node.type === 'action') emit('startConnect', props.node.id)
      else if (type === 'input' && props.node.type === 'reaction' && props.connectingFrom) emit('endConnect', props.node.id)
    }
    return { onMouseDown, onSelect, connector }
  },
  template: `<div class=\"abs-node\" :style=\"{ left: node.x + 'px', top: node.y + 'px', zIndex: isSelected ? 100 : 10 }\" @mousedown=\"onMouseDown\" @click=\"onSelect\">
    <div class=\"node-shell\" :class=\"node.type\" :style=\"{ borderColor: isSelected ? 'var(--color-accent)' : 'transparent', transform: isSelected ? 'scale(1.02)' : 'scale(1)' }\">
      <div class=\"node-top\" :style=\"{ background: node.type==='action' ? 'var(--editor-node-action-bg)' : 'var(--editor-node-reaction-bg)', color: node.type==='action' ? 'var(--editor-node-action-text)' : 'var(--editor-node-reaction-text)' }\">
        <div class=\"nt-left\">
          <div class=\"svc-icon\" :style=\"{ background: services[node.service].color }\"></div>
          <span class=\"svc-name\">{{ services[node.service].name }}</span>
        </div>
        <button class=\"no-drag del-btn\" @click.stop=\"$emit('deleteNode', node.id)\"><XIcon size=\"14\" color=\"white\" /></button>
      </div>
      <div class=\"node-body\">
        <div class=\"act-name\">{{ node.actionName }}</div>
        <div class=\"act-desc\">{{ node.desc }}</div>
      </div>
    </div>
    <div v-if=\"node.type==='reaction'\" class=\"connector input\" :style=\"{ background: connectingFrom ? 'var(--color-accent)' : '#fff' }\" @click.stop=\"connector('input')\"></div>
    <div v-if=\"node.type==='action'\" class=\"connector output\" :style=\"{ background: '#fff' }\" @click.stop=\"connector('output')\"></div>
  </div>`
}
const ConnectionLine = {
  props: { from: Number, to: Number, nodes: Array },
  computed: {
    pathData() {
      const fromNode = this.nodes.find(n => n.id === this.from)
      const toNode = this.nodes.find(n => n.id === this.to)
      if (!fromNode || !toNode) return null
      const x1 = fromNode.x + 220, y1 = fromNode.y + 50
      const x2 = toNode.x, y2 = toNode.y + 50
      const midX = (x1 + x2)/2
      return { path: `M ${x1} ${y1} C ${midX} ${y1}, ${midX} ${y2}, ${x2} ${y2}`, endX: x2, endY: y2 }
    }
  },
  template: `<svg v-if="pathData" class="conn-svg"><path :d="pathData.path" stroke="#FFB162" stroke-width="3" fill="none" /><circle :cx="pathData.endX" :cy="pathData.endY" r="6" fill="#FFB162" /></svg>`
}
const GridBackground = {
  template: `<svg class="grid-bg" xmlns="http://www.w3.org/2000/svg"><defs><pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse"><path d="M 20 0 L 0 0 0 20" fill="none" stroke="#252542" stroke-width="0.5" /></pattern></defs><rect width="100%" height="100%" fill="#1a1a2e" /><rect width="100%" height="100%" fill="url(#grid)" /></svg>`
}
const ServicePicker = {
  props: { type: String, position: Object },
  emits: ['select','close'],
  setup(props, { emit }) {
    const items = props.type === 'action' ? actionsList : reactionsList
    function choose(item) { emit('select', item) }
    function close() { emit('close') }
    return { items, choose, close, services }
  },
  template: `<div class=\"picker\" :style=\"{ left: position.x + 'px', top: position.y + 'px' }\">
    <div class=\"picker-head\" :style=\"{ background: type==='action' ? 'var(--editor-node-action-bg)' : 'var(--editor-node-reaction-bg)', color: '#fff' }\"><span class=\"ph-title\">Add {{ type==='action' ? 'Action' : 'Reaction' }}</span><button class=\"ph-close\" @click=\"close\"><XIcon size=\"18\" color=\"white\" /></button></div>
    <div class=\"picker-body\">
      <button v-for=\"(item,i) in items\" :key=\"i\" class=\"pick-row\" @click=\"choose(item)\">
        <span class=\"pick-icon\" :style=\"{ background: services[item.service].color }\"></span>
        <span class=\"pick-text\"><span class=\"pt-name\">{{ item.name }}</span><span class=\"pt-service\">{{ services[item.service].name }}</span></span>
      </button>
    </div>
  </div>`
}
</script>

<style scoped>
.editor-root { display: flex; flex-direction: column; height: 100vh; background: var(--color-canvas); }
.editor-header { height: 56px; display:flex; align-items:center; justify-content: space-between; padding:0 1rem; background: var(--color-noble-black); border-bottom:1px solid var(--color-canvas-grid); }
.title-input { background: transparent; color: var(--color-pale-cashmere); font-weight:600; font-size:1.1rem; outline: none; border: 0; border-bottom:1px solid; padding:2px 4px; }
.title-input::placeholder { color: var(--color-pale-cashmere); opacity: .7; }
.header-actions { display:flex; align-items:center; gap: .75rem; }
.mini-btn { display:flex; align-items:center; gap:.4rem; padding:.45rem .9rem; border-radius:10px; font-size:.75rem; font-weight:600; color:#fff; border:none; cursor:pointer; }
.mini-btn.action { background: var(--color-blue-estate); }
.mini-btn.reaction { background: var(--color-wahoo); }
.save-btn { background: var(--color-accent); color: var(--color-accent-contrast); padding:.55rem 1.1rem; border-radius:12px; display:flex; align-items:center; gap:.5rem; font-weight:600; border:none; cursor:pointer; }
.canvas { flex:1; position:relative; overflow:hidden; }
.grid-bg { position:absolute; inset:0; }
.grid-bg path { stroke: var(--color-canvas-grid); }
.grid-bg rect:first-child { fill: var(--color-canvas); }
.abs-node { position:absolute; cursor:move; user-select:none; }
.node-shell { width:220px; border:2px solid; border-radius:16px; overflow:hidden; box-shadow:0 12px 30px -10px rgba(0,0,0,.6); transition:transform .2s; background:#fff; }
.node-top { display:flex; align-items:center; justify-content:space-between; padding:.5rem .75rem; }
.nt-left { display:flex; align-items:center; gap:.5rem; }
.svc-icon { width:28px; height:28px; border-radius:10px; }
.svc-name { color:#fff; font-size:.75rem; font-weight:500; }
.del-btn { background:transparent; border:none; padding:4px; border-radius:6px; cursor:pointer; }
.del-btn:hover { background:rgba(255,255,255,.2); }
.node-body { padding:.7rem .9rem; }
.act-name { font-size:.8rem; font-weight:600; color: var(--color-noble-black); }
.act-desc { font-size:.65rem; margin-top:4px; color: var(--color-muted); }
.connector { position:absolute; width:16px; height:16px; border-radius:50%; border:2px solid; top:50%; transform:translateY(-50%); cursor:crosshair; transition:transform .15s; }
.connector:hover { transform:translateY(-50%) scale(1.2); }
.connector.input { left:-8px; }
.connector.output { right:-8px; }
.conn-svg { position:absolute; inset:0; pointer-events:none; z-index:5; }
.picker { position:absolute; width:280px; background:#fff; border-radius:18px; box-shadow:0 14px 40px -12px rgba(0,0,0,.55); overflow:hidden; z-index:50; }
.picker-head { display:flex; align-items:center; justify-content:space-between; padding:.75rem 1rem; }
.ph-title { color:#fff; font-weight:600; font-size:.85rem; }
.ph-close { background:transparent; border:none; cursor:pointer; display:flex; align-items:center; }
.picker-body { max-height:260px; overflow:auto; }
.pick-row { width:100%; display:flex; align-items:center; gap:.75rem; padding:.6rem .85rem; background:transparent; border:none; cursor:pointer; text-align:left; border-bottom:1px solid #eee; }
.pick-row:hover { background:#f6f7f9; }
.pick-icon { width:36px; height:36px; border-radius:12px; }
.pick-text { flex:1; }
.pt-name { font-size:.8rem; font-weight:600; color:#1B2632; }
.pt-service { font-size:.65rem; color:#A35139; }
.help { position:absolute; bottom:1rem; left:1rem; font-size:.6rem; background:rgba(0,0,0,.5); color:rgba(255,255,255,.6); padding:.5rem .75rem; border-radius:10px; }
</style>
