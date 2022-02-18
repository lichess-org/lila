import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessEditor',
    input: 'src/main.ts',
    output: 'editor',
  },
});
