import { h, VNode } from 'snabbdom';
import { onInsert, bind, dataIcon } from 'common/snabbdom';
import { storedBooleanProp } from 'common/storage';
import { rangeConfig } from 'common/controls';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';
import { Entry, VoiceMove, RootCtrl } from './interfaces';
import * as xhr from 'common/xhr';
import { onClickAway } from 'common';

let moveCtrl: VoiceMove; // globals. not just a bad idea, it's the law!

const supportedLangs = [
  ['en', 'English'],
  //['fr', 'Français'],
  //['de', 'Deutsch'],
  //['tr', 'Türkçe'],
  //['vi', 'Tiếng Việt'],
];

export { type RootCtrl, type VoiceMove } from './interfaces';

export function renderVoiceMove(redraw: () => void, isPuzzle: boolean) {
  const rec = storedBooleanProp('voice.listening', false);
  const rtfm = storedBooleanProp('voice.rtfm', false);

  return h(`div#voice-control${isPuzzle ? '.puz' : ''}`, [
    h('div#voice-status-row', [
      h(
        'button#microphone-button',
        {
          class: { enabled: lichess.mic!.isListening, busy: lichess.mic!.isBusy },
          attrs: { role: 'button', title: 'Toggle voice control' },
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
        },
        h('span.microphone-icon', {
          attrs: { ...dataIcon(lichess.mic?.isBusy ? '' : ''), title: 'Toggle voice control' },
        })
      ),
      h('span#voice-status', {
        hook: onInsert(el => lichess.mic?.addListener('moveInput', txt => (el.innerText = txt))),
      }),
      h('button#voice-help-button', {
        attrs: { role: 'button', ...dataIcon('') },
        hook: bind('click', () => moveCtrl.showHelp(true)),
      }),
      h('button#voice-settings-button', {
        attrs: { role: 'button', ...dataIcon('') },
        class: { active: moveCtrl?.showSettings() },
        hook: bind('click', () => moveCtrl.showSettings.toggle(), redraw),
      }),
    ]),
    moveCtrl?.showSettings() ? voiceSettings(redraw) : null,
    moveCtrl?.showHelp() ? helpModal() : null,
  ]);
}

export function makeVoiceMove(ctrl: RootCtrl, fen: string): VoiceMove {
  lichess.loadModule('voice.move').then(() => {
    moveCtrl = window.LichessVoiceMove(ctrl, fen);
  });
  return {
    update: fen => moveCtrl?.update(fen),
    opponentRequest: (request, callback) => moveCtrl?.opponentRequest(request, callback),
    get showHelp() {
      return moveCtrl?.showHelp;
    },
    get showSettings() {
      return moveCtrl?.showSettings;
    },
    get clarityPref() {
      return moveCtrl?.clarityPref;
    },
    get timerPref() {
      return moveCtrl?.timerPref;
    },
    get colorsPref() {
      return moveCtrl?.colorsPref;
    },
    get langPref() {
      return moveCtrl?.langPref;
    },
    get allPhrases() {
      return moveCtrl?.allPhrases;
    },
  };
}

function voiceSettings(redraw: () => void): VNode {
  return h('div#voice-settings', { hook: onInsert(onClickAway(() => moveCtrl.showSettings(false))) }, [
    h('div.voice-setting', [
      h('label', { attrs: { for: 'voice-clarity' } }, 'Clarity'),
      h('input#voice-clarity', {
        attrs: {
          type: 'range',
          min: 0,
          max: 2,
          step: 1,
        },
        hook: rangeConfig(moveCtrl.clarityPref, val => {
          moveCtrl.clarityPref(val);
          redraw();
        }),
      }),
      h('div.range_value', ['Fuzzy', 'Average', 'Clear'][moveCtrl.clarityPref()]),
    ]),
    h('div.voice-setting', [
      h('label', { attrs: { for: 'voice-timer' } }, 'Timer'),
      h('input#voice-timer', {
        attrs: {
          type: 'range',
          min: 0,
          max: 5,
          step: 1,
        },
        hook: rangeConfig(moveCtrl.timerPref, val => {
          moveCtrl.timerPref(val);
          redraw();
        }),
      }),
      h('div.range_value', ['Off', '2s', '2.5s', '3s', '4s', '5s'][moveCtrl.timerPref()]),
    ]),
    h('div.voice-choices', [
      'Label with',
      h(
        'span.btn-rack',
        ['Colors', 'Numbers'].map(pref =>
          h(
            `span.btn-rack__btn`,
            {
              class: { active: moveCtrl.colorsPref() === (pref === 'Colors') },
              hook: bind('click', () => moveCtrl.colorsPref(pref === 'Colors'), redraw),
            },
            [pref]
          )
        )
      ),
    ]),
    supportedLangs.length < 2
      ? null
      : h('div.voice-setting', [
          h('label', { attrs: { for: 'voice-lang' } }, 'Language'),
          h(
            'select#voice-lang',
            {
              attrs: { name: 'lang' },
              hook: bind('change', e => moveCtrl.langPref((e.target as HTMLSelectElement).value)),
            },
            [
              ...supportedLangs.map(l =>
                h(
                  'option',
                  {
                    attrs: l[0] === moveCtrl.langPref() ? { value: l[0], selected: '' } : { value: l[0] },
                  },
                  l[1]
                )
              ),
            ]
          ),
        ]),
    /*$('body').data('user')
      ? h(
          'a.button',
          {
            attrs: {
              title:
                'Click here to remove the microphone from your UI.\nIt can be re-enabled in Preferences -> Display',
            },
            hook: bind('click', () =>
              xhr
                .text('/pref/voice', { method: 'post', body: xhr.form({ voice: '0' }) })
                .then(() => window.location.reload())
            ),
          },
          'Hide voice controls'
        )
      : null,*/
  ]);
}

function helpModal() {
  return snabModal({
    class: `voice-move-help`,
    content: [h('div.scrollable', spinner())],
    onClose: () => moveCtrl.showHelp(false),
    onInsert: async el => {
      const [, grammar, html] = await Promise.all([
        lichess.loadCssPath('voiceMove.help'),
        xhr.jsonSimple(lichess.assetUrl(`compiled/grammar/moves-${moveCtrl.langPref()}.json`)),
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
        const all = moveCtrl.allPhrases().sort((a, b) => a[0].localeCompare(b[0]));
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
