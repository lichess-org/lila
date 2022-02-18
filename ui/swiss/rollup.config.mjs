import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessSwiss',
    input: 'src/main.ts',
    output: 'swiss',
  },
});
