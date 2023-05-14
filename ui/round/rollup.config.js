import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LishogiRound',
    input: 'src/main.ts',
    output: 'lishogi.round',
  },
  nvui: {
    name: 'NVUI',
    input: 'src/plugins/nvui.ts',
    output: 'lishogi.round.nvui',
  },
});
