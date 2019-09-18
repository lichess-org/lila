const lilaGulp = require('@build/tsProject');
const lilaGulpPlugins = require('@build/tsPlugins');

lilaGulp('LichessAnalyse', 'lichess.analyse', __dirname);

// adds commands: NVUI
lilaGulpPlugins([
  {
    standalone: 'NVUI',
    entries: ['src/plugins/nvui.ts'],
    target: 'lichess.analyse.nvui.min.js'
  }
]);
