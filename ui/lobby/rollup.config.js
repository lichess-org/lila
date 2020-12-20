import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessLobby',
    input: 'src/boot.ts',
    output: 'lobby',
  },
});
