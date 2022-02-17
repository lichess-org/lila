import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessLearn',
    input: 'src/main.ts',
    output: 'learn',
  },
});
