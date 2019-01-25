const lilaGulp = require('../gulp/tsProject.js');
const lilaGulpPlugins = require('../gulp/tsPlugins.js');

lilaGulp('LichessAnalyse', 'lichess.analyse', __dirname);

// adds commands: NVUI
lilaGulpPlugins([
  {
    standalone: 'NVUI',
    entries: ['src/plugins/nvui.ts'],
    target: 'lichess.round.nvui.min.js'
  }
]);
