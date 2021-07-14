import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessModGoal',
    input: 'src/main.ts',
    output: 'modGoal',
  },
});
