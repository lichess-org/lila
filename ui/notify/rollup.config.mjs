import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessNotify',
    input: 'src/main.ts',
    output: 'notify',
  },
});
