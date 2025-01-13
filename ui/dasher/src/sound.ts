import { debounce } from 'common/timings';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import { type Close, bind, header } from './util';

type SoundKey = string;

interface Sound {
  key: SoundKey;
  name: string;
}

export interface SoundData {
  list: Sound[];
}

export interface SoundCtrl {
  makeList(): Sound[];
  api: any;
  set(k: SoundKey): void;
  volume(v: number): void;
  redraw: Redraw;
  close: Close;
}

export function ctrl(soundData: SoundData, redraw: Redraw, close: Close): SoundCtrl {
  const api = window.lishogi.sound;

  return {
    makeList() {
      const canSpeech = window.speechSynthesis?.getVoices().length;
      return soundData.list.filter(s => s.key != 'speech' || canSpeech);
    },
    api,
    set(key: SoundKey) {
      api.speech(key == 'speech');
      window.lishogi.pubsub.emit('speech.enabled', api.speech());
      if (api.speech()) api.say({ en: 'Speech synthesis ready', jp: '音声合成の準備が整いました' });
      else {
        api.changeSet(key);
        // If we want to play move for all sets we need to get move sound for pentatonic
        if (key === 'music') api.play('genericNotify');
        else api.play('move');
        window.lishogi.xhr
          .text('POST', '/pref/soundSet', { formData: { set: key } })
          .catch(() => window.lishogi.announce({ msg: 'Failed to save sound preference' }));
      }
      redraw();
    },
    volume(v: number) {
      api.setVolume(v);
      // plays a move sound if speech is off
      api.sayOrPlay('move', { en: 'Volume set', jp: '音量が設定されました' });
    },
    redraw,
    close,
  };
}

export function view(ctrl: SoundCtrl): VNode {
  const current = ctrl.api.speech() ? 'speech' : ctrl.api.set();

  return h(
    `div.sub.sound.${ctrl.api.set()}`,
    {
      hook: {
        insert() {
          if (window.speechSynthesis) window.speechSynthesis.onvoiceschanged = ctrl.redraw;
        },
      },
    },
    [
      header(i18n('sound'), ctrl.close),
      h('div.content', [
        h('div.selector', ctrl.makeList().map(soundView(ctrl, current))),
        slider(ctrl),
      ]),
    ],
  );
}

function slider(ctrl: SoundCtrl): VNode {
  return h(
    'div.slider',
    h('input', {
      attrs: {
        id: 'sound-slider',
        type: 'range',
        min: 0,
        max: 1,
        step: 0.01,
      },
      hook: {
        insert: vnode => {
          const el = vnode.elm as HTMLInputElement;
          const setVolume = debounce(ctrl.volume, 300);

          el.value = ctrl.api.getVolume();
          el.addEventListener('input', _ => {
            const value = Number.parseFloat(el.value);
            setVolume(value);
          });
          el.addEventListener('mouseout', _ => el.blur());
        },
      },
    }),
  );
}

function soundView(ctrl: SoundCtrl, current: SoundKey) {
  return (s: Sound) =>
    h(
      'a.text',
      {
        hook: bind('click', () => ctrl.set(s.key)),
        class: { active: current === s.key },
        attrs: { 'data-icon': 'E' },
      },
      s.name,
    );
}
