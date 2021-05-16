import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'Palantir',
    input: 'src/main.ts',
    output: 'palantir',
  },
});
