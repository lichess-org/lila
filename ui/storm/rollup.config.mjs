import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessStorm',
    input: 'src/main.ts',
    output: 'storm',
  },
});
