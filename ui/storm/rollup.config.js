import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessStorm',
    input: 'src/main.ts',
    output: 'storm',
  },
});
