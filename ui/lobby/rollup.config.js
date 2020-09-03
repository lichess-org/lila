import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessLobby',
    input: 'src/main.ts',
    output: 'lobby',
  },
});
