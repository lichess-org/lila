function lichessOrchestra() {

  var soundDir = '/assets/sound/instrument/';

  var instruments = {
      celesta: [],
      clav: [],
      swells: []
    },
    noteOverlap = 20,
    noteTimeout = 300,
    currentNotes = 0,
    maxPitch = 23;

  // load celesta and clav sounds
  for (var i = 1; i <= 24; i++) {
    if (i > 9) {
      fn = 'c0' + i;
    } else {
      fn = 'c00' + i;
    }
    instruments.celesta.push(new Howl({
      src: [soundDir + 'celesta/' + fn + '.ogg', soundDir + 'celesta/' + fn + '.mp3'],
      volume: 0.3
    }))
    instruments.clav.push(new Howl({
      src: [soundDir + 'clav/' + fn + '.ogg', soundDir + 'clav/' + fn + '.mp3'],
      volume: 0.2
    }))
  }
  // load swell sounds
  for (var i = 1; i <= 3; i++) {
    instruments.swells.push(new Howl({
      src: [soundDir + 'swells/swell' + i + '.ogg', soundDir + 'swells/swell' + i + '.mp3'],
      volume: 0.8
    }))
  }

  var play = function(instrument, pitch) {
    pitch = Math.round(Math.max(0, Math.min(maxPitch, pitch)));
    if (instrument === 'swells') pitch = Math.floor(pitch / 8);
    if (currentNotes < noteOverlap) {
      currentNotes++;
      instruments[instrument][pitch].play();
      setTimeout(function() {
        currentNotes--;
      }, noteTimeout);
    }
  };

  play('swells', 0);

  return {
    play: play
  };
}
