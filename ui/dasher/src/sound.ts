import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Close, bind, header } from './util'

type Key = string;

export type Sound = string[];

export interface SoundData {
  current: Key
  list: Sound[]
}

export interface SoundCtrl {
  makeList(): Sound[];
  api: any;
  set(k: Key): void;
  volume(v: number): void;
  redraw: Redraw;
  trans: Trans;
  close: Close;
}

export function ctrl(raw: string[], trans: Trans, redraw: Redraw, close: Close): SoundCtrl {

  const list: Sound[] = raw.map(s => s.split(' '));

  const api = window.lichess.sound;

  function say(text: string) {
    const msg = new SpeechSynthesisUtterance(text);
    msg.volume = parseFloat(window.lichess.storage.get('sound-volume'));
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(msg);
  }

  return {
    makeList() {
      const canSpeech = window.speechSynthesis && window.speechSynthesis.getVoices().length;
      return list.filter(s => s[0] != 'speech' || canSpeech);
    },
    api,
    set(k: Key) {
      api.changeSet(k);
      api.genericNotify();
      $.post('/pref/soundSet', { set: k });
      redraw();
      if (k == 'speech') say('Speech synthesis ready');
    },
    volume(v: number) {
      api.setVolume(v);
      if (api.set() == 'speech') say('knight c3');
      else api.move(true);
    },
    redraw,
    trans,
    close
  };
}

export function view(ctrl: SoundCtrl): VNode {

  return h('div.sub.sound.' + ctrl.api.set(), {
    hook: {
      insert() {
        window.speechSynthesis.onvoiceschanged = ctrl.redraw;
      }
    }
  }, [
    header(ctrl.trans('sound'), ctrl.close),
    h('div.content', [
      h('div.slider', { hook: { insert: vn => makeSlider(ctrl, vn) } }),
      h('div.selector', {
        attrs: { method: 'post', action: '/pref/soundSet' }
      }, ctrl.makeList().map(soundView(ctrl, ctrl.api.set())))
    ])
  ]);
}

function makeSlider(ctrl: SoundCtrl, vnode: VNode) {
  const setVolume = window.lichess.debounce(ctrl.volume, 50);
  window.lichess.slider().done(() => {
    $(vnode.elm as HTMLElement).slider({
      orientation: 'vertical',
      min: 0,
      max: 1,
      range: 'min',
      step: 0.01,
      value: ctrl.api.volumeStorage.get() || ctrl.api.defaultVolume,
      slide: (_: any, ui: any) => setVolume(ui.value)
    });
  });
}

function soundView(ctrl: SoundCtrl, current: Key) {
  return (s: Sound) => h('a.text', {
    hook: bind('click', () => ctrl.set(s[0])),
    class: { active: current === s[0] },
    attrs: { 'data-icon': 'E' }
  }, s[1]);
}
