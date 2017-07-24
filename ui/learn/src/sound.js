var util = require('./util');

var baseUrl = util.assetUrl + 'sound/';

var make = function(file, volume) {
  var sound = new Howl({
    src: [
      baseUrl + file + '.ogg',
      baseUrl + file + '.mp3'
    ],
    volume: volume || 1
  });
  return function() {
    if (lichess.sound.set() !== 'silent') sound.play();
  };
};

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
