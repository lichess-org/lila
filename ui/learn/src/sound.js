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
    sound.play();
  };
};

module.exports = {
  move: make('standard/Move'),
  take: make('sfx/Tournament3rd'),
  stageEnd: make('other/energy3'),
  lessonEnd: make('sfx/Tournament1st'),
  failure: make('other/failure')
};
