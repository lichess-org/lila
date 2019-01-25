const lilaGulp = require('../gulp/tsProject.js');
const lilaGulpPlugins = require('../gulp/tsPlugins.js');

lilaGulp('LichessAnalyse', 'lichess.analyse', __dirname);

// adds commands: NVUI
lilaGulpPlugins([
  {
    standalone: 'NVUI',
    entries: ['src/plugins/nvui.ts'],
    target: 'lichess.analyse.nvui.min.js'
  }
]);
