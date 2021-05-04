import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LishogiPuzzle',
    input: 'src/main.ts',
    output: 'lishogi.puzzle',
  },
  dashboard: {
    name: 'LishogiPuzzleDashboard',
    input: 'src/dashboard.ts',
    output: 'lishogi.puzzle.dashboard',
  },
});
