const lidraughtsGulp = require('../gulp/tsProject.js');
const lidraughtsGulpPlugins = require('../gulp/tsPlugins.js');

lidraughtsGulp('LidraughtsRound', 'lidraughts.round', __dirname);

// adds commands: KeyboardMove, Speech, NVUI
lidraughtsGulpPlugins([
  {
    standalone: 'KeyboardMove',
    entries: ['src/plugins/keyboardMove.ts'],
    target: 'lidraughts.round.keyboardMove.min.js'
  },
  {
    standalone: 'Speech',
    entries: ['src/plugins/speech.ts'],
    target: 'lidraughts.round.speech.min.js'
  },
  {
    standalone: 'NVUI',
    entries: ['src/plugins/nvui.ts'],
    target: 'lidraughts.round.nvui.min.js'
  }
]);
