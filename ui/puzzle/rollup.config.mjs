import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessPuzzle',
    input: 'src/main.ts',
    output: 'puzzle',
  },
  dashboard: {
    name: 'NewChessPuzzleDashboard',
    input: 'src/dashboard.ts',
    output: 'puzzle.dashboard',
  },
  nvui: {
    name: 'NVUI',
    input: 'src/plugins/nvui.ts',
    output: 'puzzle.nvui',
  },
});
