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
    
    // Grid state
    const grid = ref<Cell[]>([]);
    const pathFound = ref(false);
    const animationInterval = ref<number | null>(null);
    
    // Initialize Union-Find data structure
    const initUnionFind = () => {
      const size = props.width * props.height;
      parent.value = Array(size).fill(0).map((_, i) => i);
      rank.value = Array(size).fill(0);
    };
    
    // Find operation with path compression
    const find = (x: number): number => {
      if (parent.value[x] !== x) {
        parent.value[x] = find(parent.value[x]);
      }
      return parent.value[x];
    };
    
    // Union operation with rank
    const union = (x: number, y: number): void => {
      const rootX = find(x);
      const rootY = find(y);
      
      if (rootX === rootY) return;
      
      if (rank.value[rootX] < rank.value[rootY]) {
        parent.value[rootX] = rootY;
      } else if (rank.value[rootX] > rank.value[rootY]) {
        parent.value[rootY] = rootX;
      } else {
        parent.value[rootY] = rootX;
        rank.value[rootX]++;
      }
    };
    
    // Initialize grid
    const initGrid = () => {
      grid.value = [];
      for (let row = 0; row < props.height; row++) {
        for (let col = 0; col < props.width; col++) {
          grid.value.push({
            row,
            col,
            state: 'empty',
            propagationDelay: 0
          });
        }
      }
      pathFound.value = false;
      initUnionFind();
    };
    
    // Get cell index from row and column
    const getCellIndex = (row: number, col: number): number => {
      return row * props.width + col;
    };
    
    // Check if there's a path from top to bottom
    const checkPathExists = (): boolean => {
      // Check if any cell in the top row is connected to any cell in the bottom row
      for (let topCol = 0; topCol < props.width; topCol++) {
        const topIndex = getCellIndex(0, topCol);
        if (grid.value[topIndex].state !== 'filled') continue;
        
        for (let bottomCol = 0; bottomCol < props.width; bottomCol++) {
          const bottomIndex = getCellIndex(props.height - 1, bottomCol);
          if (grid.value[bottomIndex].state !== 'filled') continue;
          
          if (find(topIndex) === find(bottomIndex)) {
            return true;
          }
        }
      }
      return false;
    };
    
    // Calculate points per second based on grid size and slider value
    const calculatePointsPerSecond = (): number => {
      const totalCells = props.width * props.height;
      
      // Convert slider value (1-100) to a percentage between 0.1% and 10% using logarithmic scale
      // Using natural log for smoother progression
      const minPercent = 0.001; // 0.1%
      const maxPercent = 0.1;   // 10%
      
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
      // Find a top cell that's connected to a bottom cell
      let startCol = -1;
      let endCol = -1;
      
      for (let topCol = 0; topCol < props.width && startCol === -1; topCol++) {
        const topIndex = getCellIndex(0, topCol);
        if (grid.value[topIndex].state !== 'filled') continue;
        
        for (let bottomCol = 0; bottomCol < props.width && endCol === -1; bottomCol++) {
          const bottomIndex = getCellIndex(props.height - 1, bottomCol);
          if (grid.value[bottomIndex].state !== 'filled') continue;
          
          if (find(topIndex) === find(bottomIndex)) {
            startCol = topCol;
            endCol = bottomCol;
          }
        }
      }
      
      if (startCol === -1 || endCol === -1) return;
      
      // Use BFS to find the path
      const visited = new Set<number>();
      const queue: { index: number, path: number[], depth: number }[] = [];
      const startIndex = getCellIndex(0, startCol);
      queue.push({ index: startIndex, path: [startIndex], depth: 0 });
      visited.add(startIndex);
      
      while (queue.length > 0) {
        const { index, path, depth } = queue.shift()!;
        const row = Math.floor(index / props.width);
        const col = index % props.width;
        
        if (row === props.height - 1 && col === endCol) {
          // Found the path
          // Calculate the total path length for delay normalization
          const totalLength = path.length;
          
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
    };
    
    // Add a random point to the grid
    const addRandomPoint = (): void => {
      if (pathFound.value || props.isPaused) return;
      
      // Find all empty cells
      const emptyCells = grid.value
        .map((cell, index) => ({ cell, index }))
        .filter(({ cell }) => cell.state === 'empty');
      
      if (emptyCells.length === 0) return;
      
      // Select a random empty cell
      const randomIndex = Math.floor(Math.random() * emptyCells.length);
      const { cell, index } = emptyCells[randomIndex];
      
      // Fill the cell
      cell.state = 'filled';
      
      // Connect with adjacent filled cells
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
            union(index, neighborIndex);
          }
        }
      }
      
      // Check if there's a path from top to bottom
      if (checkPathExists()) {
        pathFound.value = true;
        findPath();
        stopAnimation();
      }
    };
    
    // Start the animation
    const startAnimation = () => {
      if (animationInterval.value !== null) return;
      
      const interval = 1000 / calculatePointsPerSecond();
      animationInterval.value = window.setInterval(addRandomPoint, interval);
    };
    
    // Stop the animation
    const stopAnimation = () => {
      if (animationInterval.value !== null) {
        clearInterval(animationInterval.value);
        animationInterval.value = null;
      }
    };
    
    // Reset the grid
    const resetGrid = () => {
      stopAnimation();
      initGrid();
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