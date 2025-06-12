import * as licon from 'lib/licon';
import { text as xhrText, form as xhrForm } from 'lib/xhr';
import { throttle, throttlePromiseDelay } from 'lib/async';
import { h, type VNode } from 'snabbdom';
import { header } from './util';
import { bind, LooseVNodes } from 'lib/snabbdom';
import { type DasherCtrl, PaneCtrl } from './interfaces';
import { pubsub } from 'lib/pubsub';
import { isSafari } from 'lib/device';
import { snabDialog } from 'lib/view/dialog';
import { LichessStorage, storage } from 'lib/storage';

type Key = string;

export type Sound = string[];

export interface SoundData {
  current: Key;
  list: Sound[];
}

export class SoundCtrl extends PaneCtrl {
  private list: Sound[];
  private showVoiceSelection = false;
  private voiceStorage: LichessStorage = storage.make('speech.voice');

  constructor(root: DasherCtrl) {
    super(root);
    this.list = this.root.data.sound.list.map(s => s.split(' '));
  }

  render = (): VNode => {
    const current = site.sound.speech() ? 'speech' : site.sound.theme;
    if (current === 'speech' && this.showVoiceSelection) site.sound.say('Speech synthesis ready');

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
                  hook: bind('click', () => {
                    if (s[0] === 'speech') {
                      this.showVoiceSelection = true;
                      if (current === 'speech') this.redraw();
                    }
                    if (s[0] !== current) this.set(s[0]);
                  }),
                  class: { active: current === s[0] },
                  attrs: { 'data-icon': licon.Checkmark, type: 'button' },
                },
                [s[1], s[0] === 'speech' ? '...' : ''],
              ),
            ),
          ),
        ]),
        this.showVoiceSelection
          ? snabDialog({
              onClose: () => {
                this.showVoiceSelection = false;
                this.redraw();
              },
              modal: true,
              vnodes: this.renderVoiceSelection(),
              onInsert: dlg => {
                dlg.show();
              },
            })
          : undefined,
      ],
    );
  };

  private renderVoiceSelection(): LooseVNodes {
    const selectedVoice = this.voiceStorage.get() ?? '';
    const synth = window.speechSynthesis;
    let voices = synth.getVoices();
    if (voices.length !== 0) {
      voices = voices.filter(voice => voice.lang.startsWith('en'));
    }
    return [
      h('div.selector', [
        ...voices.map(voice =>
          h(
            'button.text',
            {
              hook: bind('click', event => {
                const target = event.target as HTMLElement;
                this.voiceStorage.set(target.textContent);
                this.redraw();
              }),
              class: { active: voice.name === selectedVoice },
              attrs: {
                'data-icon': voice.name === selectedVoice ? licon.Checkmark : '',
                type: 'button',
              },
            },
            voice.name,
          ),
        ),
      ]),
    ];
  }

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
