import { storage } from './storage';
import pubsub from './pubsub';
import soundBox from './soundbox';
import { soundUrl } from './assets';

const api: any = {};

let soundSet = $('body').data('sound-set');

const speechStorage = storage.makeBoolean('speech.enabled');

api.speech = v => {
  if (typeof v == 'undefined') return speechStorage.get();
  speechStorage.set(v);
  api.collection.clear();
};

function memoize(factory) {
  let loaded = {};
  const f = (key: string) => {
    if (!loaded[key]) loaded[key] = factory(key);
    return loaded[key];
  };
  f.clear = () => {
    loaded = {};
  };
  return f;
};

const volumes = {
  lowtime: 0.5,
  explosion: 0.35,
  confirmation: 0.5
};
api.collection = memoize((k: string, name?: string) => {
  let set = soundSet;
  if (set === 'music' || speechStorage.get()) {
    if (['move', 'capture', 'check'].includes(k)) return () => { };
    set = 'standard';
  }
  name = name || k.charAt(0).toUpperCase() + k.slice(1);
  soundBox.loadOggOrMp3(k, `${soundUrl}/${set}/${name}`);
  return () => soundBox.play(k, volumes[k] || 1);
});
const enabled = () => soundSet !== 'silent';
api.load = (k: string, name?: string) => {
  api[k] = (text: string) => {
    if (enabled() && (!text || !api.say(text))) api.collection(k, name)();
  }
}
api.say = (text: SpeechSynthesisUtterance | string, cut: boolean, force: boolean) => {
  if (!speechStorage.get() && !force) return false;
  const msg = typeof text == 'string' ? new SpeechSynthesisUtterance(text) : text;
  msg.volume = soundBox.getVolume();
  msg.lang = 'en-US';
  if (cut) speechSynthesis.cancel();
  speechSynthesis.speak(msg);
  return true;
};

const publish = () => pubsub.emit('sound_set', soundSet);

if (soundSet == 'music') setTimeout(publish, 500);

api.changeSet = (s: string) => {
  soundSet = s;
  api.collection.clear();
  publish();
};

api.set = () => soundSet;

export default api;
