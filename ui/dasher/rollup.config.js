import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessDasher',
    input: 'src/main.ts',
    output: 'dasher',
  },
});
