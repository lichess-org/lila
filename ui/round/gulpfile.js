const lidraughtsGulp = require('../gulp/tsProject.js');

lidraughtsGulp('LidraughtsRound', 'lidraughts.round', __dirname, ['src/plugins/keyboardMove.ts']);
