var util = require('./util');

var assetUrl;

var make = function(file, volume) {
  assetUrl = assetUrl || $('body').data('asset-url') + '/assets/';
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
  levelEnd: make('other/energy3')
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
