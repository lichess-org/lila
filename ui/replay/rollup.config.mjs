import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessReplay',
    input: 'src/main.ts',
    output: 'replay',
  },
});
