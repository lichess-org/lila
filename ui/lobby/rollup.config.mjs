import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessLobby',
    input: 'src/boot.ts',
    output: 'lobby',
  },
});
