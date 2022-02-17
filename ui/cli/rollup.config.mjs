import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessCli',
    input: 'src/main.ts',
    output: 'cli',
  },
});
