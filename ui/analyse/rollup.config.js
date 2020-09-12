import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessAnalyse',
    input: 'src/main.ts',
    output: 'analysis-board', // can't call it analyse.js, triggers adblockers :facepalm:
  },
  nvui: {
    input: 'src/plugins/nvui.ts',
    output: 'analysis-board.nvui',
  },
});
