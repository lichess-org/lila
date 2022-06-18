import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LishogiLearn',
    input: 'src/main.ts',
    output: 'lishogi.learn',
  },
});
