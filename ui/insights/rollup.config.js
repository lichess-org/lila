import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LishogiInsights',
    input: 'src/main.ts',
    output: 'lishogi.insights',
  },
});
