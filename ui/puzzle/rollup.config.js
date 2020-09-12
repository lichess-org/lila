import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessPuzzle',
    input: 'src/main.ts',
    output: 'puzzle',
  },
});
