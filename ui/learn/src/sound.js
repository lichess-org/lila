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


var sounds = {
  move: make('standard/Move'),
  take: make('sfx/Tournament3rd', 0.4),
  levelStart: make('other/ping'),
  levelEnd: make('other/energy3'),
  stageStart: make('other/guitar'),
  // stageEnd: make('sfx/Tournament1st'),
  stageEnd: make('other/gewonnen'),
  failure: make('other/failure')
};

var onces = [];
sounds.once = function(sound, id) {
  var key = id + '/' + sound;
  if (onces.indexOf(key) === -1) {
    sounds[sound]();
    onces.push(key);
  }
};

module.exports = sounds;
