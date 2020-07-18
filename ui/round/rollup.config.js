import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessRound',
    input: 'src/main.ts',
    output: 'lichess.round',
  },
  keyboardMove: {
    name: 'KeyboardMove',
    input: 'src/plugins/keyboardMove.ts',
    output: 'lichess.round.keyboardMove',
  },
  nvui: {
    name: 'NVUI',
    input: 'src/plugins/nvui.ts',
    output: 'lichess.round.nvui',
  },
});
