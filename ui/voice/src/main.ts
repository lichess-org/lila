import { h, VNode } from 'snabbdom';
import { onInsert, bind, dataIcon } from 'common/snabbdom';
import { storedBooleanProp } from 'common/storage';
import { rangeConfig } from 'common/controls';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';
import { type VoiceMove, type Entry } from './interfaces';
import { VoiceMoveCtrl } from './voiceMove';
import * as xhr from 'common/xhr';

export { makeVoiceMove } from './voiceMove';
export { type RootCtrl, type VoiceMove } from './interfaces';

export function renderVoiceMove(inCtrl: VoiceMove, isPuzzle: boolean) {
  const ctrl = inCtrl as VoiceMoveCtrl;
  const rec = storedBooleanProp('voice.listening', false);
  const rtfm = storedBooleanProp('voice.rtfm', false);

  return h(`div#voice-control${isPuzzle ? '.puz' : ''}`, [
    h('div#voice-status-row', [
      h('button#microphone-button', {
        class: { enabled: lichess.mic!.isListening, busy: lichess.mic!.isBusy },
        attrs: { role: 'button', ...dataIcon(lichess.mic?.isBusy ? '' : ''), title: 'Toggle voice control' },
        hook: onInsert(el => {
          el.addEventListener('click', _ => {
            if (!rtfm()) {
              setTimeout(() =>
                alert(`Read the help page (the 'i' button) before using your microphone to make moves.`)
              );
              rtfm(true);
            }
            rec(!(lichess.mic?.isListening || lichess.mic?.isBusy)) ? lichess.mic?.start() : lichess.mic?.stop();
          });
          if (rec() && !lichess.mic?.isListening) setTimeout(() => el.dispatchEvent(new Event('click')));
        }),
      }),
      h('span#voice-status', {
        hook: onInsert(el => lichess.mic?.addListener('moveInput', txt => (el.innerText = txt || 'some placeholder'))),
      }),
      h('button#voice-help-button', {
        attrs: { role: 'button', ...dataIcon('') },
        hook: bind('click', () => ctrl.showHelp(true)),
      }),
      h('button#voice-settings-button', {
        attrs: { role: 'button', ...dataIcon('') },
        hook: bind('click', toggleSettings.bind(undefined, true)),
      }),
    ]),
    voiceSettings(ctrl),
    ctrl.showHelp() ? helpModal(ctrl) : null,
  ]);
}

function voiceSettings(ctrl: VoiceMoveCtrl): VNode {
  return h('div#voice-settings', [
    h('div.voice-setting', [
      h('label', { attrs: { for: 'clarity' } }, 'Clarity'),
      h('input#clarity', {
        attrs: {
          type: 'range',
          min: 0,
          max: 2,
          step: 1,
        },
        hook: rangeConfig(ctrl.clarityPref, (val: number) => {
          ctrl.clarityPref(val);
          ctrl.root.redraw();
        }),
      }),
      h('div.range_value', ['Fuzzy', 'Average', 'Clear'][ctrl.clarityPref()]),
    ]),
    h('div.voice-setting', [
      h('label', { attrs: { for: 'timer' } }, 'Timer'),
      h('input#timer', {
        attrs: {
          type: 'range',
          min: 0,
          max: 5,
          step: 1,
        },
        hook: rangeConfig(ctrl.timerPref, (val: number) => {
          ctrl.timerPref(val);
          ctrl.root.redraw();
        }),
      }),
      h('div.range_value', ['Off', '2s', '2.5s', '3s', '4s', '5s'][ctrl.timerPref()]),
    ]),
    h('div.voice-choices', [
      'Label with',
      h(
        'span',
        ['Colors', 'Numbers'].map(pref =>
          h(
            `div#${pref}.choice.${ctrl.colorsPref() === (pref === 'Colors') ? 'selected' : ''}`,
            {
              hook: bind('click', () => {
                ['Colors', 'Numbers'].map(x => $(`#${x}`).toggleClass('selected'));
                ctrl.colorsPref(pref === 'Colors');
              }),
            },
            [h('label', pref)]
          )
        )
      ),
    ]),
    h('div.voice-setting', [
      h('label', { attrs: { for: 'lang' } }, 'Language'),
      h(
        'select#lang',
        {
          attrs: { name: 'lang' },
          hook: bind('change', e => ctrl.langPref((e.target as HTMLSelectElement).value)),
        },
        [
          ...ctrl.supportedLangs.map(l =>
            h(
              'option',
              {
                attrs: l[0] === ctrl.lang ? { value: l[0], selected: '' } : { value: l[0] },
              },
              l[1]
            )
          ),
        ]
      ),
    ]),
  ]);
}

let lastPointerDown = -1;
document.addEventListener('pointerdown', e => (lastPointerDown = e.timeStamp));

function toggleSettings(onButton: boolean) {
  const settingsButton = $('#voice-settings-button');
  const settingsPane = $('#voice-settings');

  function onPointerUp(e: PointerEvent) {
    const el = e.target as Element;
    if (settingsPane.get(0)?.contains(el) || settingsButton.get(0)?.contains(el) || e.timeStamp - lastPointerDown > 500)
      return;
    toggleSettings(false);
    document.removeEventListener('pointerup', onPointerUp);
  }
  // TODO - keyboard nav, nvui, use blur, something something
  document.removeEventListener('pointerup', onPointerUp);

  if (onButton || settingsButton.hasClass('active')) settingsButton.toggleClass('active');
  if (settingsButton.hasClass('active')) {
    settingsPane.show();

    setTimeout(() => document.addEventListener('pointerup', onPointerUp));
  } else settingsPane.hide();
}

function helpModal(ctrl: VoiceMoveCtrl) {
  return snabModal({
    class: `voice-move-help`,
    content: [h('div.scrollable', spinner())],
    onClose: () => ctrl.showHelp(false),
    onInsert: async el => {
      const [, grammar, html] = await Promise.all([
        lichess.loadCssPath('voiceMove.help'),
        xhr.jsonSimple(lichess.assetUrl(`compiled/grammar/moves-${ctrl.lang}.json`)),
        xhr.text(xhr.url(`/help/voice-move`, {})),
      ]);
      // using lexicon instead of crowdin translations for moves/commands
      el.find('.scrollable').html(html);
      const valToWord = (val: string, phonetic: boolean) =>
        grammar.entries.find((e: Entry) => (e.val ?? e.tok) === val && (!phonetic || e.tags?.includes('phonetic')))?.in;
      $('.val-to-word', el).each(function (this: HTMLElement) {
        const tryPhonetic = (val: string) =>
          (this.classList.contains('phonetic') && valToWord(val, true)) || valToWord(val, false);
        this.innerText = this.innerText
          .split(',')
          .map(v => tryPhonetic(v))
          .join(' ');
      });
      el.find('#all-phrases-button').on('click', () => {
        let html = '<table id="big-table"><tbody>';
        const all = ctrl.allPhrases().sort((a, b) => a[0].localeCompare(b[0]));
        const cols = Math.min(3, Math.ceil(window.innerWidth / 399));
        const rows = Math.ceil(all.length / cols);
        for (let row = 0; row < rows; row++) {
          html += '<tr>';
          for (let i = row; i < all.length; i += rows) {
            html += `<td>${all[i][0]}</td><td>${all[i][1]}</td>`;
          }
          html += '</tr>';
        }
        html += '</tbody></table>';
        el.find('.scrollable').html(html);
      });
    },
  });
}
