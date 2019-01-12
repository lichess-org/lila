const lilaGulp = require('../gulp/tsProject.js');

lilaGulp('LichessRound', 'lichess.round', __dirname, ['src/plugins/keyboardMove.ts']);
