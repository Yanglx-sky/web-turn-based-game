<template>
  <div v-if="visible" class="reconnect-dialog-overlay">
    <div class="reconnect-dialog">
      <h3>检测到未结束的战斗</h3>
      <p>是否继续战斗？</p>
      <div class="dialog-buttons">
        <button @click="continueBattle" class="btn-primary">继续战斗</button>
        <button @click="abandonBattle" class="btn-secondary">放弃战斗</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, defineEmits } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['continue', 'abandon'])

const continueBattle = () => {
  emit('continue')
}

const abandonBattle = () => {
  emit('abandon')
}
</script>

<style scoped>
.reconnect-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.reconnect-dialog {
  background-color: #fff;
  border-radius: 8px;
  padding: 2rem;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  text-align: center;
}

.reconnect-dialog h3 {
  margin-top: 0;
  color: #333;
  font-size: 1.5rem;
}

.reconnect-dialog p {
  margin: 1rem 0 2rem;
  color: #666;
  font-size: 1.1rem;
}

.dialog-buttons {
  display: flex;
  justify-content: center;
  gap: 1rem;
}

.btn-primary {
  background-color: #409EFF;
  color: #fff;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-primary:hover {
  background-color: #66B1FF;
}

.btn-secondary {
  background-color: #909399;
  color: #fff;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-secondary:hover {
  background-color: #A6A9AD;
}
</style>