import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessSwiss',
    input: 'src/main.ts',
    output: 'swiss',
  },
});
