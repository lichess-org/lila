import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessSimul',
    input: 'src/main.ts',
    output: 'simul',
  },
});
