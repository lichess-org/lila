import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    input: 'src/main.ts',
    output: 'serviceWorker',
  },
});
