import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Close, bind } from './util'

type Key = string;
type Name = string;

export type Sound = string[];

export interface SoundData {
  current: Key
  list: Sound[]
}

export interface SoundCtrl {
  current: () => Key
  dict: Sound[]
  set(k: Key): void
  trans: Trans
  close: Close
}

export function ctrl(current: Key, raw: string[], trans: Trans, redraw: Redraw, close: Close): SoundCtrl {

  const dict: Sound[] = raw.map(s => s.split(' '));

  return {
    current: () => current,
    dict,
    set(k: Key) {
      current = k;
      redraw();
    },
    trans,
    close
  };
}

export function view(ctrl: SoundCtrl): VNode {

  return h('div.sub.sound', [
    h('a.head.text', {
      attrs: { 'data-icon': 'I' },
      hook: bind('click', ctrl.close)
    }, ctrl.trans('sound')),
    h('div.content', [
      h('div.slider'),
      h('div.selector', {
        attrs: { method: 'post', action: '/pref/soundSet' }
      }, ctrl.dict.map(soundView(ctrl, ctrl.current())))
    ])
  ]);
}

function soundView(ctrl: SoundCtrl, current: Key) {
  return (s: Sound) => h('a', {
    hook: bind('click', () => ctrl.set(s[0])),
    class: { active: current === s[0] }
  }, [
    h('i', {
      attrs: { 'data-icon': 'E' }
    }),
    h('span', s[1])
  ]);
}
