import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessChat',
    input: 'src/main.ts',
    output: 'chat',
  },
});
