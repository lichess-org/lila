import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessMsg',
    input: 'src/main.ts',
    output: 'msg',
  },
});
