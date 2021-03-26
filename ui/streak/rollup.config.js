import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessStreak',
    input: 'src/main.ts',
    output: 'streak',
  },
});
