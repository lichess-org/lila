import * as licon from 'common/licon';
import * as xhr from 'common/xhr';
import throttle, { throttlePromiseDelay } from 'common/throttle';
import { h, VNode } from 'snabbdom';
import { Redraw, Close, bind, header } from './util';

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

  const postSet = throttlePromiseDelay(
    () => 1000,
    (soundSet: string) =>
      xhr
        .text('/pref/soundSet', {
          body: xhr.form({ soundSet }),
          method: 'post',
        })
        .catch(() => lichess.announce({ msg: 'Failed to save sound preference' })),
  );

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
  const current = ctrl.api.speech() ? 'speech' : ctrl.api.theme;

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
      h('div.content.force-ltr', [
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
    ],
  );
}

const soundView = (ctrl: SoundCtrl, current: Key) => (s: Sound) =>
  h(
    'button.text',
    {
      hook: bind('click', () => ctrl.set(s[0])),
      class: { active: current === s[0] },
      attrs: { 'data-icon': licon.Checkmark, type: 'button' },
    },
    s[1],
  );
