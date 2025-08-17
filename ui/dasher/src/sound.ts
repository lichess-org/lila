import * as licon from 'lib/licon';
import { text as xhrText, form as xhrForm } from 'lib/xhr';
import { throttle, throttlePromiseDelay } from 'lib/async';
import { h, type VNode } from 'snabbdom';
import { header } from './util';
import { bind, dataIcon } from 'lib/snabbdom';
import { type DasherCtrl, PaneCtrl } from './interfaces';
import { pubsub } from 'lib/pubsub';
import { isSafari } from 'lib/device';
import { snabDialog } from 'lib/view/dialog';

type Key = string;

export type Sound = string[];

export interface SoundData {
  current: Key;
  list: Sound[];
}

export class SoundCtrl extends PaneCtrl {
  private list: Sound[];
  private showVoiceSelection = false;

  constructor(root: DasherCtrl) {
    super(root);
    this.list = this.root.data.sound.list.map(s => s.split(' '));
  }

  render = (): VNode => {
    return h(
      'div.sub.sound.' + this.getCurrent(),
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
                  class: { active: this.getCurrent() === s[0] },
                  attrs: { ...dataIcon(licon.Checkmark), type: 'button' },
                },
                [s[1], s[0] === 'speech' ? '...' : ''],
              ),
            ),
          ),
        ]),
        this.voiceSelectionDialog(),
      ],
    );
  };

  private voiceSelectionDialog = () => {
    if (!this.showVoiceSelection) return;
    const content = this.renderVoiceSelection();
    if (!content) return;
    return snabDialog({
      onClose: () => {
        this.showVoiceSelection = false;
        this.redraw();
      },
      modal: true,
      vnodes: [content],
      onInsert: dlg => {
        dlg.show();
        dlg.view.querySelector('.active')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      },
    });
  };

  private getCurrent = (): Key => (site.sound.speech() ? 'speech' : site.sound.theme);

  private renderVoiceSelection(): VNode | false {
    const selectedVoice = site.sound.getVoice();
    const voiceMap = site.sound.getVoiceMap();
    return voiceMap.size < 2
      ? false
      : h(
          'div.selector',
          [...voiceMap.keys()]
            .sort((a, b) => a.localeCompare(b))
            .map(name =>
              h(
                'button.text',
                {
                  hook: bind('click', event => {
                    const target = event.target as HTMLElement;
                    site.sound.setVoice(voiceMap.get(target.textContent!)!);
                    site.sound.say('Speech synthesis ready');
                    this.redraw();
                  }),
                  class: { active: name === selectedVoice?.name },
                  attrs: {
                    ...dataIcon(name === selectedVoice?.name ? licon.Checkmark : ''),
                    type: 'button',
                  },
                },
                name,
              ),
            ),
        );
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
    if (site.sound.speech()) {
      this.showVoiceSelection = true;
      site.sound.say('Speech synthesis ready');
      site.sound.changeSet('speech');
      this.postSet('speech');
    } else {
      site.sound.changeSet(k);
      site.sound.play('genericNotify');
      this.postSet(k);
    }
    pubsub.emit('speech.enabled', site.sound.speech());
    this.redraw();
  };

  private volume = (v: number) => {
    site.sound.setVolume(v);
    // plays a move sound if speech is off
    site.sound.sayOrPlay('move', 'knight F 7');
  };
}
