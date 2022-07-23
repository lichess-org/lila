import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessOpening',
    input: 'src/main.ts',
    output: 'opening',
  },
});
