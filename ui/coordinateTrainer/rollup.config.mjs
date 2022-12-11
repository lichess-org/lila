import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessCoordinateTrainer',
    input: 'src/main.ts',
    output: 'coordinateTrainer',
  },
});
