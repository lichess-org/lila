import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessDgt',
    input: 'src/main.ts',
    output: 'dgt',
  },
});
