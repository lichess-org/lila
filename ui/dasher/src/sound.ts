import * as licon from 'common/licon';
import { text as xhrText, form as xhrForm } from 'common/xhr';
import { throttle, throttlePromiseDelay } from 'common/timing';
import { h, type VNode } from 'snabbdom';
import { header } from './util';
import { bind } from 'common/snabbdom';
import { type DasherCtrl, PaneCtrl } from './interfaces';
import { pubsub } from 'common/pubsub';
import { isSafari } from 'common/device';

type Key = string;

export type Sound = string[];

export interface SoundData {
  current: Key;
  list: Sound[];
}

export class SoundCtrl extends PaneCtrl {
  private list: Sound[];
  constructor(root: DasherCtrl) {
    super(root);
    this.list = this.root.data.sound.list.map(s => s.split(' '));
  }

  render = (): VNode => {
    const current = site.sound.speech() ? 'speech' : site.sound.theme;

    return h(
      'div.sub.sound.' + current,
      {
        hook: {
          insert: () => {
            if (window.speechSynthesis) window.speechSynthesis.onvoiceschanged = this.redraw;
          },
        },
      },
      [
        header(i18n.site.sound, this.close),
        h('div.content.force-ltr', [
          h('input', {
            attrs: {
              type: 'range',
              min: 0,
              max: 1,
              step: 0.01,
              value: site.sound.getVolume(),
              orient: 'vertical',
              style: isSafari({ below: '18' }) ? 'appearance: slider-vertical' : '',
            },
            hook: {
              insert: vnode => {
                const input = vnode.elm as HTMLInputElement,
                  setVolume = throttle(150, this.volume);
                $(input).on('input', () => setVolume(parseFloat(input.value)));
              },
            },
          }),
          h(
            'div.selector',
            this.makeList().map(s =>
              h(
                'button.text',
                {
                  hook: bind('click', () => this.set(s[0])),
                  class: { active: current === s[0] },
                  attrs: { 'data-icon': licon.Checkmark, type: 'button' },
                },
                s[1],
              ),
            ),
          ),
        ]),
      ],
    );
  };

  private postSet = throttlePromiseDelay(
    () => 1000,
    (soundSet: string) =>
      xhrText('/pref/soundSet', { body: xhrForm({ soundSet }), method: 'post' }).catch(() =>
        site.announce({ msg: 'Failed to save sound preference' }),
      ),
  );

  private makeList = () => {
    const canSpeech = window.speechSynthesis?.getVoices().length;
    return this.list.filter(s => s[0] !== 'speech' || canSpeech);
  };

  private set = (k: Key) => {
    site.sound.speech(k === 'speech');
    pubsub.emit('speech.enabled', site.sound.speech());
    if (site.sound.speech()) {
      site.sound.changeSet('standard');
      this.postSet('standard');
      site.sound.say('Speech synthesis ready');
    } else {
      site.sound.changeSet(k);
      site.sound.play('genericNotify');
      this.postSet(k);
    }
    this.redraw();
  };

  private volume = (v: number) => {
    site.sound.setVolume(v);
    // plays a move sound if speech is off
    site.sound.sayOrPlay('move', 'knight F 7');
  };
}
