import { h } from 'snabbdom';
import * as licon from 'common/licon';
import { onInsert, bind } from 'common/snabbdom';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';
import * as xhr from 'common/xhr';
import { onClickAway } from 'common';
import { Entry, VoiceCtrl } from './interfaces';
import { supportedLangs } from './main';

export function renderVoiceBar(ctrl: VoiceCtrl, redraw: () => void, cls?: string) {
  return h(`div#voice-bar${cls ? '.' + cls : ''}`, [
    h('div#voice-status-row', [
      h('button#microphone-button', {
        hook: onInsert(el => el.addEventListener('click', () => ctrl.toggle())),
      }),
      h('span#voice-status', {
        hook: onInsert(el => lichess.mic.setController(voiceBarUpdater(ctrl, el))),
      }),
      h('button#voice-help-button', {
        attrs: { 'data-icon': licon.InfoCircle, title: 'Voice help' },
        hook: bind('click', () => ctrl.showHelp(true)),
      }),
      h('button#voice-settings-button', {
        attrs: { 'data-icon': licon.Gear, title: 'Voice settings' },
        class: { active: ctrl.showPrefs() },
        hook: bind('click', () => ctrl.showPrefs.toggle(), redraw),
      }),
    ]),
    ctrl.showPrefs()
      ? h('div#voice-settings', { hook: onInsert(onClickAway(() => ctrl.showPrefs(false))) }, [
          deviceSelector(ctrl, redraw),
          langSetting(ctrl),
          ...(ctrl.module()?.prefNodes(redraw) ?? []),
          pushTalkSetting(ctrl),
          h('br'),
          ctrl.moduleId === 'move' ? voiceDisable() : null,
        ])
      : null,

    ctrl.showHelp() ? renderHelpModal(ctrl) : null,
  ]);
}

function voiceBarUpdater(ctrl: VoiceCtrl, el: HTMLElement) {
  const voiceBtn = $('button#microphone-button');

  return (txt: string, tpe: Voice.MsgType) => {
    const classes: [string, boolean][] = [];
    classes.push(['listening', lichess.mic.isListening]);
    classes.push(['busy', lichess.mic.isBusy]);
    classes.push(['push-to-talk', ctrl.pushTalk() && !lichess.mic.isListening && !lichess.mic.isBusy]);
    classes.map(([clz, has]) => (has ? voiceBtn.addClass(clz) : voiceBtn.removeClass(clz)));
    voiceBtn.attr('data-icon', lichess.mic.isBusy ? licon.Cancel : licon.Voice);

    if (tpe !== 'partial') el.innerText = txt;
  };
}

function pushTalkSetting(ctrl: VoiceCtrl) {
  return h('div.voice-setting', { attrs: { style: 'align-self: center' } }, [
    h('div.switch', { attrs: { title: 'Hold the shift key while speaking' } }, [
      h('input#wake-mode.cmn-toggle', {
        attrs: { type: 'checkbox', checked: ctrl.pushTalk() },
        hook: bind('change', e => ctrl.pushTalk((e.target as HTMLInputElement).checked)),
      }),
      h('label', { attrs: { for: 'wake-mode' } }),
    ]),
    h('label', { attrs: { for: 'wake-mode' } }, ['Push ', h('strong', 'shift'), ' key to talk']),
  ]);
}

function langSetting(ctrl: VoiceCtrl) {
  return supportedLangs.length < 2
    ? null
    : h('div.voice-setting', [
        h('label', { attrs: { for: 'voice-lang' } }, 'Language'),
        h(
          'select#voice-lang',
          {
            attrs: { name: 'lang' },
            hook: bind('change', e => ctrl.lang((e.target as HTMLSelectElement).value)),
          },
          [
            ...supportedLangs.map(l =>
              h(
                'option',
                {
                  attrs: l[0] === ctrl.lang() ? { value: l[0], selected: '' } : { value: l[0] },
                },
                l[1]
              )
            ),
          ]
        ),
      ]);
}

let devices: InputDeviceInfo[] | undefined;
function deviceSelector(ctrl: VoiceCtrl, redraw: () => void) {
  return h('div.voice-setting', [
    h('label', { attrs: { for: 'voice-mic' } }, 'Microphone'),
    h(
      'select#voice-mic',
      {
        hook: onInsert((el: HTMLSelectElement) => {
          el.addEventListener('change', () => ctrl.micId(el.value));
          if (devices === undefined)
            lichess.mic.getMics().then(ds => {
              devices = ds;
              redraw();
            });
        }),
      },
      (devices || []).map(d =>
        h(
          'option',
          {
            attrs: {
              value: d.deviceId,
              selected: d.deviceId === ctrl.micId(),
            },
          },
          d.label
        )
      )
    ),
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

function renderHelpModal(ctrl: VoiceCtrl) {
  const showMoveList = (el: Cash) => {
    let html = '<table id="big-table"><tbody>';
    const all =
      ctrl
        .module()
        ?.allPhrases()
        ?.sort((a, b) => a[0].localeCompare(b[0])) ?? [];
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
  };
  return snabModal({
    class: `voice-move-help`,
    content: [h('div.scrollable', spinner())],
    onClose: () => ctrl.showHelp(false),
    onInsert: async el => {
      const [, grammar, html] = await Promise.all([
        lichess.loadCssPath('voiceMove.help'),
        ctrl.moduleId !== 'coords'
          ? xhr.jsonSimple(lichess.assetUrl(`compiled/grammar/${ctrl.moduleId}-${ctrl.lang()}.json`))
          : Promise.resolve({ entries: [] }),
        xhr.text(xhr.url(`/help/voice/${ctrl.moduleId}`, {})),
      ]);

      if (ctrl.showHelp() === 'list') {
        showMoveList(el);
        return;
      }
      // using lexicon instead of crowdin translations for moves/commands
      el.find('.scrollable').html(html);
      const valToWord = (val: string, phonetic: boolean) =>
        grammar.entries.find(
          (e: Entry) => (e.val ?? e.tok) === val && (!phonetic || e.tags?.includes('phonetic'))
        )?.in;
      $('.val-to-word', el).each(function (this: HTMLElement) {
        const tryPhonetic = (val: string) =>
          (this.classList.contains('phonetic') && valToWord(val, true)) || valToWord(val, false);
        this.innerText = this.innerText
          .split(',')
          .map(v => tryPhonetic(v))
          .join(' ');
      });
      el.find('#all-phrases-button').on('click', () => showMoveList(el));
    },
  });
}
