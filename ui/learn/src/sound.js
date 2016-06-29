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
    if ($.sound.set() !== 'silent') sound.play();
  };
};

module.exports = {
  move: make('standard/Move'),
  take: make('sfx/Tournament3rd', 0.7),
  stageStart: make('other/ping'),
  stageEnd: make('other/energy3'),
  lessonStart: make('other/guitar'),
  // lessonEnd: make('sfx/Tournament1st'),
  lessonEnd: make('other/gewonnen'),
  failure: make('other/failure')
};
