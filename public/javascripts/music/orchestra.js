function lishogiOrchestra() {
  var soundDir = lishogi.assetUrl('sound/instrument/', { noVersion: true });

  var makeSoundPair = function (sound) {
    return [soundDir + sound + '.ogg', soundDir + sound + '.mp3'];
  };

  var instruments = {
      celesta: [],
      clav: [],
      swells: [],
    },
    noteOverlap = 15,
    noteTimeout = 300,
    currentNotes = 0,
    maxPitch = 23;

  // load celesta and clav sounds
  for (var i = 1; i <= 24; i++) {
    if (i > 9) fn = 'c0' + i;
    else fn = 'c00' + i;
    instruments.celesta.push(
      new Howl({
        src: makeSoundPair('celesta/' + fn),
        volume: 0.3,
      })
    );
    instruments.clav.push(
      new Howl({
        src: makeSoundPair('clav/' + fn),
        volume: 0.2,
      })
    );
  }
  // load swell sounds
  for (var i = 1; i <= 3; i++) {
    instruments.swells.push(
      new Howl({
        src: makeSoundPair('swells/swell' + i),
        volume: 0.5,
      })
    );
  }

  var play = function (instrument, pitch) {
    pitch = Math.round(Math.max(0, Math.min(maxPitch, pitch)));
    if (instrument === 'swells') pitch = Math.floor(pitch / 8);
    if (currentNotes < noteOverlap) {
      currentNotes++;
      instruments[instrument][pitch].play();
      setTimeout(function () {
        currentNotes--;
      }, noteTimeout);
    }
  };

  play('swells', 0);

  return {
    play: play,
  };
}
