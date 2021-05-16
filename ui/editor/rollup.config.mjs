import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessEditor',
    input: 'src/main.ts',
    output: 'editor',
  },
});
