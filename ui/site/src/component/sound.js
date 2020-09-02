lichess.sound = (() => {
  const api = {};
  let soundSet = $('body').data('sound-set');

  const speechStorage = lichess.storage.makeBoolean('speech.enabled');
  api.speech = v => {
    if (typeof v == 'undefined') return speechStorage.get();
    speechStorage.set(v);
    api.collection.clear();
  };

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
    berserk: 'Berserk',
    check: 'Check',
    newChallenge: 'NewChallenge',
    newPM: 'NewPM',
    confirmation: 'Confirmation',
    error: 'Error'
  };

  const volumes = {
    lowtime: 0.5,
    explode: 0.35,
    confirmation: 0.5
  };
  api.collection = new memoize(k => {
    let set = soundSet;
    if (set === 'music' || speechStorage.get()) {
      if (['move', 'capture', 'check'].includes(k)) return $.noop;
      set = 'standard';
    }
    lichess.soundBox.loadOggOrMp3(k, `${lichess.soundUrl}/${set}/${names[k]}`);
    return () => lichess.soundBox.play(k, volumes[k] || 1);
  });
  const enabled = () => soundSet !== 'silent';
  api.load = (name, file) => {
    if (!names[name]) names[name] = file;
    api[name] = text => {
      if (enabled() && (!text || !api.say(text))) api.collection(name)();
    }
  }
  Object.keys(names).forEach(api.load);
  api.say = (text, cut, force) => {
    if (!speechStorage.get() && !force) return false;
    const msg = text.text ? text : new SpeechSynthesisUtterance(text);
    msg.volume = lichess.soundBox.getVolume();
    msg.lang = 'en-US';
    if (cut) speechSynthesis.cancel();
    speechSynthesis.speak(msg);
    // console.log(`%c${msg.text}`, 'color: blue');
    return true;
  };

  const publish = () => lichess.pubsub.emit('sound_set', soundSet);
  if (soundSet == 'music') setTimeout(publish, 500);

  api.changeSet = s => {
    soundSet = s;
    api.collection.clear();
    publish();
  };

  api.set = () => soundSet;
  return api;
})();
