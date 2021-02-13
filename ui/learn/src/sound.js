var util = require('./util');

const make = (file, volume) => {
  lichess.soundBox.loadOggOrMp3(file, `${window.lichess.soundUrl}/${file}`);
  return () => {
    if (lichess.sound.set() !== 'silent') lichess.soundBox.play(file, volume);
  };
}

module.exports = {
  move: make('standard/Move'),
  take: make('sfx/Tournament3rd', 0.4),
  levelStart: make('other/ping'),
  levelEnd: make('other/energy3'),
  stageStart: make('other/guitar'),
  // stageEnd: make('sfx/Tournament1st'),
  stageEnd: make('other/gewonnen'),
  failure: make('other/no-go')
};
