import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessCli',
    input: 'src/main.ts',
    output: 'cli',
  },
});
