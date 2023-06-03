import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { onInsert, bind } from 'common/snabbdom';
import { storedBooleanProp } from 'common/storage';
import { rangeConfig } from 'common/controls';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';
import { Entry, VoiceMove, RootCtrl } from './interfaces';
import * as xhr from 'common/xhr';
import { onClickAway } from 'common';

const supportedLangs = [
  ['en', 'English'],
  //['fr', 'Français'],
  //['de', 'Deutsch'],
  //['tr', 'Türkçe'],
  //['vi', 'Tiếng Việt'],
];

export { type RootCtrl, type VoiceMove } from './interfaces';
export { makeVoiceMove };

export function renderVoiceMove(moveCtrl: VoiceMove, redraw: () => void, isPuzzle: boolean) {
  const rec = storedBooleanProp('voice.listening', false);

  const toggle = () => {
    const recId = moveCtrl.wakePref() ? 'idle' : undefined;
    if (lichess.once('voice.rtfm')) moveCtrl.showHelp(true);
    rec(lichess.mic.recId === lichess.mic.isBusy) ? lichess.mic.start(recId) : lichess.mic.stop();
  };
  if (rec() && !lichess.mic.recId) toggle();

  return h(`div#voice-control${isPuzzle ? '.puz' : ''}`, [
    h('div#voice-status-row', [
      h('button#microphone-button', {
        class: {
          listening: lichess.mic.recId !== false && lichess.mic.recId !== 'idle',
          idle: lichess.mic.recId === 'idle',
          busy: lichess.mic.isBusy,
        },
        attrs: {
          'data-icon': lichess.mic.isBusy ? licon.Cancel : licon.Voice,
          title: 'Toggle voice control',
        },
        hook: onInsert(el => el.addEventListener('click', toggle)),
      }),
      h('span#voice-status', {
        hook: onInsert(el => lichess.mic.setController(updateVoiceBar(moveCtrl, el))),
      }),
      h('button#voice-help-button', {
        attrs: { 'data-icon': licon.InfoCircle, title: 'Voice help' },
        hook: bind('click', () => moveCtrl.showHelp(true)),
      }),
      h('button#voice-settings-button', {
        attrs: { 'data-icon': licon.Gear, title: 'Voice settings' },
        class: { active: moveCtrl.showSettings() },
        hook: bind('click', () => moveCtrl.showSettings.toggle(), redraw),
      }),
    ]),
    moveCtrl.showSettings() ? renderSettings(moveCtrl, redraw) : null,
    moveCtrl.showHelp() ? helpModal(moveCtrl) : null,
  ]);
}

function updateVoiceBar(moveCtrl: VoiceMove, el: HTMLElement) {
  const voiceBtn = $('button#microphone-button');

  return (txt: string, tpe: Voice.MsgType) => {
    lichess.mic.recId !== false && lichess.mic.recId !== 'idle'
      ? voiceBtn.addClass('listening')
      : voiceBtn.removeClass('listening');
    lichess.mic.recId === 'idle' ? voiceBtn.addClass('idle') : voiceBtn.removeClass('idle');
    if (lichess.mic.isBusy && !lichess.mic.recId) {
      voiceBtn.addClass('busy');
      voiceBtn.attr('data-icon', licon.Cancel);
    } else {
      voiceBtn.removeClass('busy');
      voiceBtn.attr('data-icon', licon.Voice);
    }
    if (tpe === 'start') {
      el.innerText = '';
      if (moveCtrl.wakePref()) {
        if (txt === 'idle') voiceBtn.addClass('idle');
        else voiceBtn.removeClass('idle');
      }
    } else if (tpe !== 'partial') el.innerText = txt;
  };
}

function renderSettings(moveCtrl: VoiceMove, redraw: () => void): VNode {
  return h('div#voice-settings', { hook: onInsert(onClickAway(() => moveCtrl.showSettings(false))) }, [
    colorsSetting(moveCtrl, redraw),
    claritySetting(moveCtrl, redraw),
    timerSetting(moveCtrl, redraw),
    langSetting(moveCtrl),
    wakeSetting(moveCtrl, redraw),
    h('hr'),
    voiceDisable(),
  ]);
}

function colorsSetting(moveCtrl: VoiceMove, redraw: () => void) {
  return h('div.voice-choices', [
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
  ]);
}

function claritySetting(moveCtrl: VoiceMove, redraw: () => void) {
  return h('div.voice-setting', [
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
  ]);
}

function timerSetting(moveCtrl: VoiceMove, redraw: () => void) {
  return h('div.voice-setting', [
    h('label', { attrs: { for: 'voice-timer' } }, 'Timer'),
    h('input#voice-timer', {
      attrs: { type: 'range', min: 0, max: 5, step: 1 },
      hook: rangeConfig(moveCtrl.timerPref, val => {
        moveCtrl.timerPref(val);
        redraw();
      }),
    }),
    h('div.range_value', ['Off', '1.5s', '2s', '2.5s', '3s', '5s'][moveCtrl.timerPref()]),
  ]);
}

function langSetting(moveCtrl: VoiceMove) {
  return supportedLangs.length < 2
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
      ]);
}

function wakeSetting(moveCtrl: VoiceMove, redraw: () => void) {
  return h('div.voice-setting', { attrs: { title: '' } }, [
    h('div.switch', { attrs: { title: 'Say "hey lichess" to activate' } }, [
      h('input#wake-mode.cmn-toggle', {
        attrs: { type: 'checkbox', checked: moveCtrl.wakePref() },
        hook: bind('change', e => moveCtrl.wakePref((e.target as HTMLInputElement).checked), redraw),
      }),
      h('label', { attrs: { for: 'wake-mode' } }),
    ]),
    h('label', { attrs: { for: 'wake-mode' } }, ['Wake on ', h('strong', '"Hey Lichess"')]),
  ]);
}

function voiceDisable() {
  return !$('body').data('user')
    ? null
    : h(
        'a.button',
        {
          attrs: {
            title: 'Also set in Preferences -> Display',
          },
          hook: bind('click', () =>
            xhr
              .text('/pref/voice', { method: 'post', body: xhr.form({ voice: '0' }) })
              .then(() => window.location.reload())
          ),
        },
        'Disable voice recognition'
      );
}

function helpModal(moveCtrl: VoiceMove) {
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

async function makeVoiceMove(ctrl: RootCtrl, fen: string): Promise<VoiceMove> {
  const moveCtrl = await lichess.loadModule('voice.move').then(() => window.LichessVoiceMove(ctrl, fen));
  return {
    update: fen => moveCtrl?.update(fen),
    opponentRequest: (request, callback) => moveCtrl?.opponentRequest(request, callback),
    get showHelp() {
      return moveCtrl.showHelp;
    },
    get showSettings() {
      return moveCtrl.showSettings;
    },
    get clarityPref() {
      return moveCtrl.clarityPref;
    },
    get timerPref() {
      return moveCtrl.timerPref;
    },
    get colorsPref() {
      return moveCtrl.colorsPref;
    },
    get wakePref() {
      return moveCtrl.wakePref;
    },
    get showPromotion() {
      return moveCtrl.showPromotion;
    },
    get langPref() {
      return moveCtrl.langPref;
    },
    get allPhrases() {
      return moveCtrl.allPhrases;
    },
  };
}
