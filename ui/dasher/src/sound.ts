import * as licon from 'common/licon';
import * as xhr from 'common/xhr';
import throttle, { throttlePromiseDelay } from 'common/throttle';
import { h, VNode } from 'snabbdom';
import { Close, header } from './util';
import { bind, Redraw } from 'common/snabbdom';

type Key = string;

export type Sound = string[];

export interface SoundData {
  current: Key;
  list: Sound[];
}

export class SoundCtrl {
  list: Sound[];
  api: SoundI = lichess.sound; // ???

  constructor(
    raw: string[],
    readonly trans: Trans,
    readonly redraw: Redraw,
    readonly close: Close,
  ) {
    this.list = raw.map(s => s.split(' '));
  }

  private postSet = throttlePromiseDelay(
    () => 1000,
    (soundSet: string) =>
      xhr
        .text('/pref/soundSet', {
          body: xhr.form({ soundSet }),
          method: 'post',
        })
        .catch(() => lichess.announce({ msg: 'Failed to save sound preference' })),
  );

  makeList = () => {
    const canSpeech = window.speechSynthesis?.getVoices().length;
    return this.list.filter(s => s[0] != 'speech' || canSpeech);
  };
  set = (k: Key) => {
    this.api.speech(k == 'speech');
    lichess.pubsub.emit('speech.enabled', this.api.speech());
    if (this.api.speech()) {
      this.api.changeSet('standard');
      this.postSet('standard');
      this.api.say('Speech synthesis ready');
    } else {
      this.api.changeSet(k);
      this.api.play('genericNotify');
      this.postSet(k);
    }
    this.redraw();
  };
  volume = (v: number) => {
    this.api.setVolume(v);
    // plays a move sound if speech is off
    this.api.sayOrPlay('move', 'knight F 7');
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
