var util = require('./util');

const make = (file, volume) => {
  lichess.sound.loadOggOrMp3(file, `${lichess.sound.baseUrl}/${file}`);
  return () => lichess.sound.play(file, volume);
};

module.exports = {
  move: () => lichess.sound.play('move'),
  take: make('sfx/Tournament3rd', 0.4),
  levelStart: make('other/ping'),
  levelEnd: make('other/energy3'),
  stageStart: make('other/guitar'),
  stageEnd: make('other/gewonnen'),
  failure: make('other/no-go'),
};
