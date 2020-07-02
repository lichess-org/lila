import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessSimul',
    input: 'src/main.js',
    output: 'lichess.simul',
    js: true,
  },
});
