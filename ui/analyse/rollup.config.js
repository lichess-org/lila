import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessAnalyse',
    input: 'src/main.ts',
    output: 'analyse',
  },
  nvui: {
    input: 'src/plugins/nvui.ts',
    output: 'analyse.nvui',
  },
});
