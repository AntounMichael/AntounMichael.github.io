<template>
  <div class="relative">
    <div 
      class="grid gap-1" 
      :style="{ 
        gridTemplateColumns: `repeat(${width}, minmax(0, 1fr))`,
        gridTemplateRows: `repeat(${height}, minmax(0, 1fr))`
      }"
    >
      <div 
        v-for="(cell, index) in grid" 
        :key="index" 
        :class="[
          'aspect-square transition-colors duration-200',
          cell.state === 'empty' ? 'bg-matrix-black' : 
          cell.state === 'filled' ? 'bg-matrix-green' : 
          cell.state === 'path' ? 'bg-matrix-red' : ''
        ]"
        :style="cell.state === 'path' ? { 
          animation: `propagate 2s ease-in-out ${cell.propagationDelay}s infinite` 
        } : {}"
      ></div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, reactive, watch, onMounted, onUnmounted, computed, PropType } from 'vue';

type CellState = 'empty' | 'filled' | 'path';

interface Cell {
  row: number;
  col: number;
  state: CellState;
  propagationDelay: number;
}

export default defineComponent({
  name: 'UnionFindGrid',
  props: {
    width: {
      type: Number,
      required: true
    },
    height: {
      type: Number,
      required: true
    },
    pointsPerSecond: {
      type: Number,
      required: true
    },
    isPaused: {
      type: Boolean,
      default: false
    }
  },
  emits: [],
  setup(props, { emit }) {
    // Union-Find data structure
    const parent = ref<number[]>([]);
    const rank = ref<number[]>([]);
    const connectsToBottom = ref<boolean[]>([]);
    
    // Grid state
    const grid = ref<Cell[]>([]);
    const pathFound = ref(false);
    const animationInterval = ref<number | null>(null);
    
    // Maintain a list of empty cell indices
    const emptyCellIndices = ref<number[]>([]);
    
    // Initialize Union-Find data structure
    const initUnionFind = () => {
      const size = props.width * props.height;
      parent.value = Array(size).fill(0).map((_, i) => i);
      rank.value = Array(size).fill(0);
      connectsToBottom.value = Array(size).fill(false);
      
      // Mark bottom row cells as connecting to bottom
      for (let col = 0; col < props.width; col++) {
        const bottomIndex = getCellIndex(props.height - 1, col);
        connectsToBottom.value[bottomIndex] = true;
      }
    };
    
    // Find operation with path compression
    const find = (x: number): number => {
      if (parent.value[x] !== x) {
        const root = find(parent.value[x]);
        // Path compression
        parent.value[x] = root;
      }
      return parent.value[x];
    };
    
    // Union operation with rank and bottom connectivity propagation
    const union = (x: number, y: number): void => {
      const rootX = find(x);
      const rootY = find(y);
      
      if (rootX === rootY) return;
      
      // Union by rank
      if (rank.value[rootX] < rank.value[rootY]) {
        parent.value[rootX] = rootY;
        // Propagate bottom connectivity
        connectsToBottom.value[rootY] = connectsToBottom.value[rootY] || connectsToBottom.value[rootX];
      } else {
        parent.value[rootY] = rootX;
        // Propagate bottom connectivity
        connectsToBottom.value[rootX] = connectsToBottom.value[rootX] || connectsToBottom.value[rootY];
        
        if (rank.value[rootX] === rank.value[rootY]) {
          rank.value[rootX]++;
        }
      }
    };
    
    // Initialize grid
    const initGrid = () => {
      grid.value = [];
      emptyCellIndices.value = []; // Reset empty cell indices
      
      for (let row = 0; row < props.height; row++) {
        for (let col = 0; col < props.width; col++) {
          const index = row * props.width + col;
          grid.value.push({
            row,
            col,
            state: 'empty',
            propagationDelay: 0
          });
          emptyCellIndices.value.push(index); // Add all cells as empty initially
        }
      }
      pathFound.value = false;
      initUnionFind();
    };
    
    // Get cell index from row and column
    const getCellIndex = (row: number, col: number): number => {
      return row * props.width + col;
    };
    
    // Check if there's a path from top to bottom - O(width) operation
    const checkPathExists = (): boolean => {
      const startTime = performance.now();
      
      // Check if any cell in the top row is connected to the bottom
      for (let topCol = 0; topCol < props.width; topCol++) {
        const topIndex = getCellIndex(0, topCol);
        if (grid.value[topIndex].state !== 'filled') continue;
        
        const root = find(topIndex);
        if (connectsToBottom.value[root]) {
          const endTime = performance.now();
          //console.log(`checkPathExists runtime: ${endTime - startTime}ms`);
          return true;
        }
      }
      
      const endTime = performance.now();
      //console.log(`checkPathExists runtime: ${endTime - startTime}ms`);
      return false;
    };
    
    // Calculate points per second based on grid size and slider value
    const calculatePointsPerSecond = (): number => {
      const totalCells = props.width * props.height;
      
      // Convert slider value (1-100) to a percentage between 0.1% and 10% using logarithmic scale
      // Using natural log for smoother progression
      const minPercent = 0.005; 
      const maxPercent = 0.8;   
      
      // Normalize slider value to 0-1 range
      const normalizedValue = (props.pointsPerSecond - 1) / 99;
      
      // Apply logarithmic scaling
      // When normalizedValue is 0.5 (middle of slider), we want to get ~1% (geometric mean of min and max)
      const logScale = Math.exp(normalizedValue * (Math.log(maxPercent) - Math.log(minPercent)) + Math.log(minPercent));
      
      // Calculate actual points per second
      return Math.max(1, Math.round(totalCells * logScale));

    };
    
    // Find the path from top to bottom
    const findPath = (): void => {
      const startTime = performance.now();
      
      // Find a top cell that's connected to the bottom
      let startCol = -1;
      
      for (let topCol = 0; topCol < props.width; topCol++) {
        const topIndex = getCellIndex(0, topCol);
        if (grid.value[topIndex].state !== 'filled') continue;
        
        const root = find(topIndex);
        if (connectsToBottom.value[root]) {
          startCol = topCol;
          break;
        }
      }
      
      if (startCol === -1) return;
      
      const bfsStartTime = performance.now();
      // console.log(`Finding start column took: ${bfsStartTime - startTime}ms`);
      
      // Use BFS to find the path
      const visited = new Set<number>();
      const queue: { index: number, path: number[], depth: number }[] = [];
      const startIndex = getCellIndex(0, startCol);
      queue.push({ index: startIndex, path: [startIndex], depth: 0 });
      visited.add(startIndex);
      
      let iterations = 0;
      while (queue.length > 0) {
        iterations++;
        const { index, path, depth } = queue.shift()!;
        const row = Math.floor(index / props.width);
        const col = index % props.width;
        
        // If we've reached the bottom row, we've found a path
        if (row === props.height - 1) {
          // Calculate the total path length for delay normalization
          const totalLength = path.length;
          
          const pathMarkingStartTime = performance.now();
          // Set propagation delays based on position in path
          for (let i = 0; i < path.length; i++) {
            const cellIndex = path[i];
            grid.value[cellIndex].state = 'path';
            
            // Calculate normalized position from top (0) to bottom (1)
            const cellRow = Math.floor(cellIndex / props.width);
            const normalizedPosition = cellRow / (props.height - 1);
            
            // Use normalized position for delay (0 at top, 1 at bottom)
            // This creates a wave-like effect from top to bottom
            grid.value[cellIndex].propagationDelay = normalizedPosition * 0.5;
          }
          const pathMarkingEndTime = performance.now();
          
          const endTime = performance.now();
          // console.log(`BFS iterations: ${iterations}, path length: ${path.length}`);
          // console.log(`Path marking took: ${pathMarkingEndTime - pathMarkingStartTime}ms`);
          // console.log(`Total findPath runtime: ${endTime - startTime}ms`);
          return;
        }
        
        // Check neighbors (up, right, down, left)
        const directions = [
          { dr: -1, dc: 0 }, // up
          { dr: 0, dc: 1 },  // right
          { dr: 1, dc: 0 },  // down
          { dr: 0, dc: -1 }  // left
        ];
        
        for (const { dr, dc } of directions) {
          const newRow = row + dr;
          const newCol = col + dc;
          
          if (
            newRow >= 0 && newRow < props.height &&
            newCol >= 0 && newCol < props.width
          ) {
            const newIndex = getCellIndex(newRow, newCol);
            
            if (
              !visited.has(newIndex) &&
              grid.value[newIndex].state === 'filled' &&
              find(index) === find(newIndex)
            ) {
              visited.add(newIndex);
              queue.push({
                index: newIndex,
                path: [...path, newIndex],
                depth: depth + 1
              });
            }
          }
        }
      }
      
      const endTime = performance.now();
      // console.log(`BFS completed with no path found, iterations: ${iterations}`);
      // console.log(`Total findPath runtime: ${endTime - startTime}ms`);
    };
    
    // Start the animation
    const startAnimation = () => {
      if (animationInterval.value !== null) return;
      
      const startTime = performance.now();
      
      // Calculate points per second
      const pointsPerSec = calculatePointsPerSecond();
      
      // Set a minimum update interval (in ms) for better performance
      const MIN_UPDATE_INTERVAL = 16; // ~60fps
      
      // Calculate how many points to add per batch to maintain the desired points per second
      // while ensuring we don't update too frequently
      const batchSize = Math.max(1, Math.ceil(pointsPerSec * (MIN_UPDATE_INTERVAL / 1000)));
      
      // Calculate the actual interval needed with this batch size
      const interval = Math.max(MIN_UPDATE_INTERVAL, Math.floor(1000 * batchSize / pointsPerSec));
      
      // console.log("Grid size:", props.width, "x", props.height, "=", props.width * props.height, "cells");
      // console.log("Points per second:", pointsPerSec);
      // console.log("Batch size:", batchSize);
      // console.log("Update interval (ms):", interval);
      
      // Store animation state
      const animationState = {
        lastFrameTime: performance.now(),
        interval: interval,
        batchSize: batchSize,
        accumulatedTime: 0
      };
      
      // Animation frame handler using requestAnimationFrame for smoother animation
      const animationFrame = (timestamp: number) => {
        if (pathFound.value || props.isPaused) {
          animationInterval.value = null;
          return;
        }
        
        const elapsed = timestamp - animationState.lastFrameTime;
        animationState.accumulatedTime += elapsed;
        
        // Only process a batch if enough time has passed
        if (animationState.accumulatedTime >= animationState.interval) {
          // Process one or more batches if we've fallen behind
          const batchesToProcess = Math.floor(animationState.accumulatedTime / animationState.interval);
          
          // If we're falling significantly behind, log a warning
          if (batchesToProcess > 1) {
            //console.warn(`⚠️ Animation falling behind: Processing ${batchesToProcess} batches at once`);
          }
          
          // Process batches (limit to 3 max to prevent freezing on severe lag)
          const actualBatches = Math.min(batchesToProcess, 3);
          for (let i = 0; i < actualBatches; i++) {
            addPointBatch(animationState.batchSize);
            
            // If path found during batch processing, exit early
            if (pathFound.value) {
              animationInterval.value = null;
              return;
            }
          }
          
          // Subtract the time used (only count the batches we actually processed)
          animationState.accumulatedTime -= actualBatches * animationState.interval;
        }
        
        animationState.lastFrameTime = timestamp;
        
        // Request next frame
        animationInterval.value = requestAnimationFrame(animationFrame);
      };
      
      // Start the animation loop
      animationInterval.value = requestAnimationFrame(animationFrame);
    };
    
    // Add a batch of random points to the grid
    const addPointBatch = (batchSize: number): void => {
      if (pathFound.value || props.isPaused) return;
      
      const batchStartTime = performance.now();
      
      // Log the number of empty cells (no need to find them anymore)
      // console.log(`Empty cells available: ${emptyCellIndices.value.length}`);

      // Process up to batchSize points or until a path is found
      for (let i = 0; i < batchSize && emptyCellIndices.value.length > 0; i++) {
        // Select a random empty cell
        const randomArrayIndex = Math.floor(Math.random() * emptyCellIndices.value.length);
        const cellIndex = emptyCellIndices.value[randomArrayIndex];
        
        // Remove the selected index using the efficient swap-and-pop method
        // Swap with the last element, then pop (both O(1) operations)
        emptyCellIndices.value[randomArrayIndex] = emptyCellIndices.value[emptyCellIndices.value.length - 1];
        emptyCellIndices.value.pop();
        
        // Get the cell from the grid
        const cell = grid.value[cellIndex];
        
        // Fill the cell
        cell.state = 'filled';
        
        // Connect with adjacent filled cells
        const unionStartTime = performance.now();
        const row = cell.row;
        const col = cell.col;
        
        // Check neighbors (up, right, down, left)
        const directions = [
          { dr: -1, dc: 0 }, // up
          { dr: 0, dc: 1 },  // right
          { dr: 1, dc: 0 },  // down
          { dr: 0, dc: -1 }  // left
        ];
        
        for (const { dr, dc } of directions) {
          const newRow = row + dr;
          const newCol = col + dc;
          
          if (
            newRow >= 0 && newRow < props.height &&
            newCol >= 0 && newCol < props.width
          ) {
            const neighborIndex = getCellIndex(newRow, newCol);
            
            if (grid.value[neighborIndex].state === 'filled') {
              union(cellIndex, neighborIndex);
            }
          }
        }
        const unionEndTime = performance.now();
        
        if (i === 0) { // Only log once per batch
          // console.log(`Union operations took: ${unionEndTime - unionStartTime}ms`);
        }
      }
      
      // Check if there's a path from top to bottom after all points have been added
      if (checkPathExists()) {
        pathFound.value = true;
        
        const findPathStartTime = performance.now();
        findPath();
        const findPathEndTime = performance.now();
        // console.log(`Finding path took: ${findPathEndTime - findPathStartTime}ms`);
        
        stopAnimation();
        return; // Exit early if path is found
      }
      
      const batchEndTime = performance.now();
      // console.log(`Total batch processing took: ${batchEndTime - batchStartTime}ms for ${batchSize} points`);
    };
    
    // Stop the animation
    const stopAnimation = () => {
      if (animationInterval.value !== null) {
        cancelAnimationFrame(animationInterval.value);
        animationInterval.value = null;
      }
    };
    
    // Reset the grid
    const resetGrid = () => {
      stopAnimation();
      initGrid();
      // Ensure we're not in a paused state after reset
      pathFound.value = false;
      startAnimation();
    };
    
    // Watch for changes in props
    watch(() => props.width, resetGrid);
    watch(() => props.height, resetGrid);
    watch(() => props.pointsPerSecond, () => {
      if (animationInterval.value !== null && !props.isPaused && !pathFound.value) {
        stopAnimation();
        startAnimation();
      }
    });
    
    watch(() => props.isPaused, (isPaused) => {
      if (isPaused) {
        stopAnimation();
      } else if (!pathFound.value) {
        startAnimation();
      }
    });
    
    // Lifecycle hooks
    onMounted(() => {
      initGrid();
      startAnimation();
    });
    
    onUnmounted(() => {
      stopAnimation();
    });
    
    return {
      grid,
      pathFound,
      resetGrid
    };
  }
});
</script> 