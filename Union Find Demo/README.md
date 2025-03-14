# Union Find Algorithm Visualization

An interactive visualization of the Union-Find algorithm, demonstrating how points connect to form a path from the top to the bottom of a grid.

## Features

- Adjustable grid dimensions (width and height)
- Adjustable animation speed
- Pause/Resume and Reset controls
- Matrix/terminal theme
- Highlights the path once found

## How It Works

1. The grid starts empty (all cells are black)
2. Points are randomly filled (turn green) one by one
3. When a point is filled, it connects with any adjacent filled points (up, down, left, right)
4. The animation continues until a path is found from the top row to the bottom row
5. Once a path is found, it's highlighted and the animation stops

## Union-Find Algorithm

This visualization uses the Union-Find data structure with path compression and union by rank to efficiently track connected components in the grid.

## Getting Started

### Prerequisites

- Node.js (v14 or later)
- npm or yarn

### Installation

1. Clone the repository
2. Navigate to the project directory
3. Install dependencies:

```bash
npm install
# or
yarn
```

### Running the Application

```bash
npm run dev
# or
yarn dev
```

Open your browser and navigate to `http://localhost:5173` to see the visualization.

## Built With

- Vue 3
- TypeScript
- Tailwind CSS
- Vite 