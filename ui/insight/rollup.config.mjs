import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessInsight',
    input: 'src/main.ts',
    output: 'insight',
  },
});
