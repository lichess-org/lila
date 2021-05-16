import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessNotify',
    input: 'src/main.ts',
    output: 'notify',
  },
});
