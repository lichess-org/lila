lichess.sound = (() => {
  const api = {};
  let soundSet = $('body').data('sound-set');

  const speechStorage = lichess.storage.makeBoolean('speech.enabled');
  api.speech = v => {
    if (typeof v == 'undefined') return speechStorage.get();
    speechStorage.set(v);
    collection.clear();
  };
  api.volumeStorage = lichess.storage.make('sound-volume');

  function memoize(factory) {
    let loaded = {};
    const f = key => {
      if (!loaded[key]) loaded[key] = factory(key);
      return loaded[key];
    };
    f.clear = () => {
      loaded = {};
    };
    return f;
  };

  const names = {
    genericNotify: 'GenericNotify',
    move: 'Move',
    capture: 'Capture',
    explode: 'Explosion',
    lowtime: 'LowTime',
    victory: 'Victory',
    defeat: 'Defeat',
    draw: 'Draw',
    tournament1st: 'Tournament1st',
    tournament2nd: 'Tournament2nd',
    tournament3rd: 'Tournament3rd',
    tournamentOther: 'TournamentOther',
    berserk: 'Berserk',
    check: 'Check',
    newChallenge: 'NewChallenge',
    newPM: 'NewPM',
    confirmation: 'Confirmation',
    error: 'Error'
  };
  for (let i = 0; i <= 10; i++) names['countDown' + i] = 'CountDown' + i;

  const volumes = {
    lowtime: 0.5,
    explode: 0.35,
    confirmation: 0.5
  };
  const collection = new memoize(k => {
    let set = soundSet;
    if (set === 'music' || speechStorage.get()) {
      if (['move', 'capture', 'check'].includes(k)) return $.noop;
      set = 'standard';
    }
    const baseUrl = lichess.assetUrl('sound', {
      noVersion: true
    });
    lichess.soundBox.loadOggOrMp3(k, `${baseUrl}/${set}/${names[k]}`);
    return () => lichess.soundBox.play(k, (volumes[k] || 1) * api.getVolume());
  });
  const enabled = () => soundSet !== 'silent';
  Object.keys(names).forEach(name => {
    api[name] = text => {
      if (enabled() && (!text || !api.say(text))) collection(name)();
    }
  });
  api.say = (text, cut, force) => {
    if (!speechStorage.get() && !force) return false;
    const msg = text.text ? text : new SpeechSynthesisUtterance(text);
    msg.volume = api.getVolume();
    msg.lang = 'en-US';
    if (cut) speechSynthesis.cancel();
    speechSynthesis.speak(msg);
    // console.log(`%c${msg.text}`, 'color: blue');
    return true;
  };
  api.load = name => {
    if (enabled() && name in names) collection(name);
  };
  api.setVolume = api.volumeStorage.set;
  api.getVolume = () => {
    // garbage has been stored stored by accident (e972d5612d)
    const v = parseFloat(api.volumeStorage.get());
    return v >= 0 ? v : 0.7;
  }

  const publish = () => lichess.pubsub.emit('sound_set', soundSet);
  if (soundSet == 'music') setTimeout(publish, 500);

  api.changeSet = s => {
    soundSet = s;
    collection.clear();
    publish();
  };

  api.set = () => soundSet;
  return api;
})();
