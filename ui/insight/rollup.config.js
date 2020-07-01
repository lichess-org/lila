import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessInsight',
    input: 'src/main.js',
    output: 'lichess.insight',
    js: true,
  },
});
