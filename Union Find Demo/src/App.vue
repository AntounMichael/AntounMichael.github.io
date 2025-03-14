<template>
  <div class="min-h-screen p-6">
    <header class="mb-8 text-center">
      <h1 class="text-4xl mb-2 font-bold">Union Find Algorithm Demo</h1>
      <p class="text-xl text-gray-300">Watch as points connect to form a path from top to bottom</p>
    </header>
    
    <div class="max-w-6xl mx-auto">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2">
          <div class="border border-matrix-green p-2 rounded-lg bg-black">
            <UnionFindGrid 
              :key="resetKey"
              :width="gridWidth" 
              :height="gridHeight" 
              :pointsPerSecond="pointsPerSecond"
              :isPaused="isPaused"
            />
          </div>
        </div>
        
        <div class="bg-black bg-opacity-90 p-6 rounded-lg border border-matrix-green">
          <h2 class="text-2xl mb-4 font-bold">Controls</h2>
          
          <div class="mb-4">
            <label class="block mb-2">Grid Width: {{ gridWidth }}</label>
            <input 
              type="range" 
              min="10" 
              max="100" 
              v-model.number="gridWidth" 
              class="slider"
            />
          </div>
          
          <div class="mb-4">
            <label class="block mb-2">Grid Height: {{ gridHeight }}</label>
            <input 
              type="range" 
              min="10" 
              max="100" 
              v-model.number="gridHeight" 
              class="slider"
            />
          </div>
          
          <div class="mb-6">
            <label class="block mb-2">Animation Speed</label>
            <input 
              type="range" 
              min="1" 
              max="100" 
              v-model.number="pointsPerSecond" 
              class="slider"
            />
          </div>
          
          <div class="flex space-x-4">
            <button @click="togglePause" class="btn flex-1">
              {{ isPaused ? 'Resume' : 'Pause' }}
            </button>
            <button @click="resetGrid" class="btn flex-1">
              Reset
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue';
import UnionFindGrid from './components/UnionFindGrid.vue';

export default defineComponent({
  name: 'App',
  components: {
    UnionFindGrid
  },
  setup() {
    const gridWidth = ref(30);
    const gridHeight = ref(30);
    const pointsPerSecond = ref(50);
    const isPaused = ref(false);
    const resetKey = ref(0);
    
    const togglePause = () => {
      isPaused.value = !isPaused.value;
    };
    
    const resetGrid = () => {
      isPaused.value = false;
      resetKey.value++;
    };
    
    return {
      gridWidth,
      gridHeight,
      pointsPerSecond,
      isPaused,
      resetKey,
      togglePause,
      resetGrid
    };
  }
});
</script> 