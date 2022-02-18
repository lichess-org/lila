import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessRacer',
    input: 'src/main.ts',
    output: 'racer',
  },
});
