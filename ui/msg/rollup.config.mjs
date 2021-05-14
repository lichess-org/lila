import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessMsg',
    input: 'src/main.ts',
    output: 'msg',
  },
});
