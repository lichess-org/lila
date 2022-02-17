import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessDasher',
    input: 'src/main.ts',
    output: 'dasher',
  },
});
