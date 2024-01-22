import { VNode, h } from 'snabbdom';
import { Close, Redraw, bind, header } from './util';

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
  trans: Trans;
  close: Close;
}

export function ctrl(soundData: SoundData, trans: Trans, redraw: Redraw, close: Close): SoundCtrl {
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
      if (api.speech()) api.say({ en: 'Speech synthesis ready' });
      else {
        api.changeSet(key);
        // If we want to play move for all sets we need to get move sound for pentatonic
        if (key === 'music') api.genericNotify();
        else api.move();
        $.post('/pref/soundSet', { set: key }).fail(() =>
          window.lishogi.announce({ msg: 'Failed to save sound preference' })
        );
      }
      redraw();
    },
    volume(v: number) {
      api.setVolume(v);
      // plays a move sound if speech is off
      api.move('pawn 7 F');
    },
    redraw,
    trans,
    close,
  };
}

export function view(ctrl: SoundCtrl): VNode {
  const current = ctrl.api.speech() ? 'speech' : ctrl.api.set();

  return h(
    'div.sub.sound.' + ctrl.api.set(),
    {
      hook: {
        insert() {
          if (window.speechSynthesis) window.speechSynthesis.onvoiceschanged = ctrl.redraw;
        },
      },
    },
    [
      header(ctrl.trans.noarg('sound'), ctrl.close),
      h('div.content', [
        h('div.slider', { hook: { insert: vn => makeSlider(ctrl, vn) } }),
        h('div.selector', ctrl.makeList().map(soundView(ctrl, current))),
      ]),
    ]
  );
}

function makeSlider(ctrl: SoundCtrl, vnode: VNode) {
  const setVolume = window.lishogi.debounce(ctrl.volume, 50);
  window.lishogi.slider().done(() => {
    $(vnode.elm as HTMLElement).slider({
      orientation: 'vertical',
      min: 0,
      max: 1,
      range: 'min',
      step: 0.01,
      value: ctrl.api.getVolume(),
      slide: (_: any, ui: any) => setVolume(ui.value),
    });
  });
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
      s.name
    );
}
