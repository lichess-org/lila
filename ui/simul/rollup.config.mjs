import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessSimul',
    input: 'src/main.ts',
    output: 'simul',
  },
});
