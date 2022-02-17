import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessDgt',
    input: 'src/main.ts',
    output: 'dgt',
  },
});
