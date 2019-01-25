const lidraughtsGulp = require('../gulp/tsProject.js');
const lidraughtsGulpPlugins = require('../gulp/tsPlugins.js');

lidraughtsGulp('LidraughtsAnalyse', 'lidraughts.analyse', __dirname);

// adds commands: NVUI
lidraughtsGulpPlugins([
  {
    standalone: 'NVUI',
    entries: ['src/plugins/nvui.ts'],
    target: 'lidraughts.analyse.nvui.min.js'
  }
]);