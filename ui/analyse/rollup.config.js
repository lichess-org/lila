import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessAnalyse',
    input: 'src/main.ts',
    output: 'lichess.analyse',
  },
  nvui: {
    input: 'src/plugins/nvui.ts',
    output: 'lichess.analyse.nvui',
  },
});
