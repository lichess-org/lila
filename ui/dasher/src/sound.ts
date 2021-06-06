import { h, VNode } from 'snabbdom';
import { Redraw, Close, bind, header } from './util';
import throttle from 'common/throttle';
import * as xhr from 'common/xhr';

type Key = string;

export type Sound = string[];

export interface SoundData {
  current: Key;
  list: Sound[];
}

export interface SoundCtrl {
  makeList(): Sound[];
  api: SoundI;
  set(k: Key): void;
  volume(v: number): void;
  redraw: Redraw;
  trans: Trans;
  close: Close;
}

export function ctrl(raw: string[], trans: Trans, redraw: Redraw, close: Close): SoundCtrl {
  const list: Sound[] = raw.map(s => s.split(' '));

  const api = lichess.sound;

  const postSet = (set: string) =>
    xhr
      .text('/pref/soundSet', {
        body: xhr.form({ set }),
        method: 'post',
      })
      .catch(() => lichess.announce({ msg: 'Failed to save sound preference' }));

  return {
    makeList() {
      const canSpeech = window.speechSynthesis?.getVoices().length;
      return list.filter(s => s[0] != 'speech' || canSpeech);
    },
    api,
    set(k: Key) {
      api.speech(k == 'speech');
      lichess.pubsub.emit('speech.enabled', api.speech());
      if (api.speech()) {
        api.changeSet('standard');
        postSet('standard');
        api.say('Speech synthesis ready');
      } else {
        api.changeSet(k);
        api.play('genericNotify');
        postSet(k);
      }
      redraw();
    },
    volume(v: number) {
      api.setVolume(v);
      // plays a move sound if speech is off
      api.sayOrPlay('move', 'knight F 7');
    },
    redraw,
    trans,
    close,
  };
}

export function view(ctrl: SoundCtrl): VNode {
  const current = ctrl.api.speech() ? 'speech' : ctrl.api.soundSet;

  return h(
    'div.sub.sound.' + current,
    {
      hook: {
        insert() {
          if (window.speechSynthesis) window.speechSynthesis.onvoiceschanged = ctrl.redraw;
        },
      },
    },
    [
      header(ctrl.trans('sound'), ctrl.close),
      h('div.content', [
        h('input', {
          attrs: {
            type: 'range',
            min: 0,
            max: 1,
            step: 0.01,
            value: ctrl.api.getVolume(),
            orient: 'vertical',
          },
          hook: {
            insert(vnode) {
              const input = vnode.elm as HTMLInputElement,
                setVolume = throttle(150, ctrl.volume);
              $(input).on('input', () => setVolume(parseFloat(input.value)));
            },
          },
        }),
        h('div.selector', ctrl.makeList().map(soundView(ctrl, current))),
      ]),
    ]
  );
}

function soundView(ctrl: SoundCtrl, current: Key) {
  return (s: Sound) =>
    h(
      'a.text',
      {
        hook: bind('click', () => ctrl.set(s[0])),
        class: { active: current === s[0] },
        attrs: { 'data-icon': '' },
      },
      s[1]
    );
}
