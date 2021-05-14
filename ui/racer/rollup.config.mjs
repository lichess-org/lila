import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessRacer',
    input: 'src/main.ts',
    output: 'racer',
  },
});
