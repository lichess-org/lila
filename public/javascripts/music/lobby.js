function lidraughtsLobbyMusic() {

  var orchestra;

  lidraughts.loadScript('/assets/javascripts/music/orchestra.js').then(function() {
    orchestra = lidraughtsOrchestra();
  });

  // 1200 -> 0
  // 2200 -> 23
  var ratingToPitch = function(rating) {
    return Math.round((rating - 1200) / 1000 * 23);
  }

  var newHook = function(data) {
    var pitch = ratingToPitch(data.rating);
    var instrument;
    switch (data.perf) {
      case 'Blitz':
        instrument = 'clav';
        break;
      case 'Bullet':
      case 'Classical':
        instrument = 'celesta';
        break;
      default:
        instrument = 'swells';
        break;
    }
    orchestra.play(instrument, pitch);
  };

  if (lidraughts.once('lobby-music')) lidraughts.shepherd(function(theme) {
      var tour = new Shepherd.Tour({
        defaults: {
          classes: theme,
          scrollTo: false
        }
      });
      tour.addStep('music', {
        title: 'Music from lidraughts seeks',
        text: "Blitz seeks play the clavier.<br>" +
        "Classical and bullet play celesta.<br>" +
        "Variants play the swells.<br>" +
        "Rating determines the pitch.",
        attachTo: '#hooks_wrap left',
        buttons: [{
          text: 'OK',
          action: tour.next
        }],
      });
      tour.start();
  });

  return {
    receive: function(type, data) {
      if (!orchestra) return;
      if (type === 'had' && data.rating) newHook(data);
    }
  };
};
