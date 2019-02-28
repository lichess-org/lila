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
  list: Sound[]
  api: any,
  set(k: Key): void
  volume(v: number): void
  trans: Trans
  close: Close
}

export function ctrl(raw: string[], trans: Trans, redraw: Redraw, close: Close): SoundCtrl {

  const list: Sound[] = raw.map(s => s.split(' '));

  const api = window.lichess.sound;

  return {
    list,
    api,
    set(k: Key) {
      api.changeSet(k);
      api.genericNotify();
      $.post('/pref/soundSet', { set: k }, window.lichess.reloadOtherTabs);
      redraw();
    },
    volume(v: number) {
      api.setVolume(v);
      api.move(true);
    },
    trans,
    close
  };
}

export function view(ctrl: SoundCtrl): VNode {

  return h('div.sub.sound.' + ctrl.api.set(), [
    header(ctrl.trans('sound'), ctrl.close),
    h('div.content', [
      h('div.slider', { hook: { insert: vn => makeSlider(ctrl, vn) } }),
      h('div.selector', {
        attrs: { method: 'post', action: '/pref/soundSet' }
      }, ctrl.list.map(soundView(ctrl, ctrl.api.set())))
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
