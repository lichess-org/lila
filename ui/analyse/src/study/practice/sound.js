var baseUrl;

var make = function(file) {
  baseUrl = baseUrl || $('body').data('asset-url') + '/assets/sound/';
  var sound = new Howl({
    src: [
      baseUrl + file + '.ogg',
      baseUrl + file + '.mp3'
    ]
  });
  return function() {
    if ($.sound.set() !== 'silent') sound.play();
  };
};

module.exports = function() {
  return {
    success: make('other/energy3'),
    failure: make('other/failure2')
  };
};
