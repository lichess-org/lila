const lilaGulp = require('../gulp/tsProject.js');
const lilaGulpPlugins = require('../gulp/tsPlugins.js');

lilaGulp('LichessRound', 'lichess.round', __dirname);

// adds commands: KeyboardMove, Speech, NVUI
lilaGulpPlugins([
  {
    standalone: 'KeyboardMove',
    entries: ['src/plugins/keyboardMove.ts'],
    target: 'lichess.round.keyboardMove.min.js'
  },
  {
    standalone: 'NVUI',
    entries: ['src/plugins/nvui.ts'],
    target: 'lichess.round.nvui.min.js'
  }
]);
