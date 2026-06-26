import { frag } from 'lib';
import { isTouchDevice } from 'lib/device';
import { licon } from 'lib/licon';
import { domDialog, type Dialog } from 'lib/view';

import type AnalyseCtrl from '../ctrl';
import type { SettingsCtrl, SettingKey } from '../settingsCtrl';

type Listener = (e: Event, ctrl: SettingsCtrl, key: SettingKey) => void;

type Setting = {
  label: string;
  group: string;
  shortcutHtml?: string;
  helpHtml?: string;
  renderHtml?: (ctrl: SettingsCtrl) => string; // otherwise defaultToggleHtml
  listener?: Listener | { events: string[]; action: Listener }; // otherwise defaultToggleListener
};

const settings: Record<SettingKey, Setting> = {
  showStaticAnalysis: {
    label: i18n.preferences.showServerAnalysis,
    shortcutHtml: '<kbd>z</kbd>',
    group: i18n.preferences.generalSettings,
    helpHtml: videoHtml('info-static-analysis'),
  },
  showGauge: {
    label: i18n.preferences.showGauge,
    group: i18n.preferences.generalSettings,
    helpHtml: imageHtml('info-evaluation-gauge'),
  },
  inline: {
    label: i18n.preferences.inlineNotation,
    shortcutHtml: '<kbd>shift</kbd> +<kbd>i</kbd>',
    group: i18n.preferences.moveListSettings,
    helpHtml: videoHtml('info-inline-notation'),
  },
  disclosureMode: {
    label: i18n.preferences.disclosureMode,
    group: i18n.preferences.moveListSettings,
    helpHtml: videoHtml('info-disclosure-mode'),
  },
  showLiveGlyphs: {
    label: i18n.preferences.showLiveGlyphs,
    group: i18n.preferences.moveListSettings,
    helpHtml: videoHtml('info-live-annotations'),
  },
  showBestMoveArrows: {
    label: i18n.preferences.showBestMoveArrows,
    shortcutHtml: '<kbd>a</kbd>',
    group: i18n.preferences.boardSettings,
    helpHtml: videoHtml('info-best-move-arrows'),
  },
  showVariationArrows: {
    label: i18n.preferences.showVariationArrows,
    shortcutHtml: '<kbd>v</kbd>',
    group: i18n.preferences.boardSettings,
    helpHtml: $html`
      ${videoHtml('info-variation-arrows')}
      <span>${i18n.site.keyCycleSelectedVariation} <kbd>shift</kbd></span>`,
  },
  showManeuverMoveArrows: {
    label: i18n.preferences.showManeuverArrows,
    group: i18n.preferences.boardSettings,
    helpHtml: $html`
      ${imageHtml('info-maneuver-arrows')}
      <span>${i18n.preferences.maneuverArrowsHelp}</span>`,
  },
  showMoveAnnotationsOnBoard: {
    label: i18n.preferences.showMoveAnnotationsOnBoard,
    group: i18n.preferences.boardSettings,
    helpHtml: videoHtml('info-move-annotations-on-board'),
  },
  showUndefendedPieces: {
    label: i18n.preferences.showUndefendedPieces,
    group: i18n.preferences.boardSettings,
    helpHtml: videoHtml('info-undefended-pieces'),
  },
  showPinnedPieces: {
    label: i18n.preferences.showPinnedPieces,
    group: i18n.preferences.boardSettings,
    helpHtml: videoHtml('info-pinned-pieces'),
  },
  showCheckableKing: {
    label: i18n.preferences.showCheckableKing,
    group: i18n.preferences.boardSettings,
    helpHtml: videoHtml('info-checkable-king'),
  },
};

export async function showSettingsDialog(ctrl: AnalyseCtrl): Promise<Dialog> {
  return domDialog({
    class: 'analysis-settings-dialog',
    htmlText: '<h2>Analysis settings</h2>',
    append: [{ node: settingsView(ctrl.settings) }],
    modal: !isTouchDevice(),
    easyClose: 'clickOutside',
    show: true,
    actions: [
      { selector: '.show-all', result: 'showKeyboardShortcuts' },
      { selector: '.ok', result: 'ok' },
    ],
    onClose: dlg => {
      if (dlg.returnValue !== 'showKeyboardShortcuts') return;
      ctrl.keyboardHelp = true;
      ctrl.redraw();
    },
  });
}

export function settingsView(ctrl: SettingsCtrl): HTMLElement {
  const groupedHtml = (group: keyof typeof i18n.preferences) => {
    return $html`
      <fieldset>
        <legend>${i18n.preferences[group]}</legend>
        ${Object.keys(settings)
          .filter((key: SettingKey): key is SettingKey => settings[key].group === i18n.preferences[group])
          .map(key => (settings[key].renderHtml ?? defaultToggleHtml)(ctrl, key))
          .join('')}
      </fieldset>`;
  };
  const view = frag<HTMLElement>($html`
    <div class="analysis-settings-view">
      <div class="column">
        ${helpHtml()}
        ${groupedHtml('generalSettings')}
      </div>
      <div class="column">
        ${groupedHtml('moveListSettings')}
        ${groupedHtml('boardSettings')}
      </div>
    </div>`);

  if (isTouchDevice()) setupTouchHelp(view);
  else setupHoverHelp(view);

  view.querySelectorAll<HTMLInputElement>('.setting input').forEach(input => {
    const key = input.dataset.key as SettingKey;
    if (!settings[key]) return;
    const listener = settings[key].listener ?? defaultToggleListener;
    if ('events' in listener) {
      listener.events.forEach(event => input.addEventListener(event, e => listener.action(e, ctrl, key)));
    } else {
      input.addEventListener('change', e => listener(e, ctrl, key));
    }
  });
  return view;
}

function setupTouchHelp(view: HTMLElement) {
  view.querySelectorAll<HTMLElement>('.help-button').forEach(el => {
    const key = el.dataset.key as SettingKey;
    if (!settings[key]) return;
    const htmlText = settings[key].helpHtml;

    el.addEventListener('click', () =>
      domDialog({ htmlText, class: 'setting-popup', noCloseButton: true, show: true, easyClose: 'anyClick' }),
    );
  });
}

function setupHoverHelp(view: HTMLElement) {
  const helpEl = () => view.querySelector<HTMLElement>('.help-container')!.firstElementChild!;
  const helpPanes = { keyboardHelp: helpEl() } as Record<string, Element>;

  let hoverTimeout: number;
  view.querySelectorAll<HTMLElement>('.hover-help').forEach(el => {
    const key = el.dataset.key as SettingKey;
    const setting = settings[key];
    if (!setting?.helpHtml) return;

    el.addEventListener('mouseenter', () => {
      clearTimeout(hoverTimeout);
      hoverTimeout = setTimeout(
        () => {
          const helpPaneEl =
            helpPanes[key] ??
            frag<HTMLElement>($html`
              <fieldset class="help-pane" data-key="${key}">
                <legend>${setting.label}</legend>
                ${setting.helpHtml}
              </fieldset>`);
          helpEl().replaceWith(helpPaneEl);
          if (helpPanes[key]) helpPaneEl.querySelector<HTMLVideoElement>('video')?.play();
          helpPanes[key] = helpPaneEl;
        },
        helpEl() === helpPanes.keyboardHelp ? 400 : 0,
      );
    });
    el.addEventListener('mouseleave', () => {
      clearTimeout(hoverTimeout);
      hoverTimeout = setTimeout(() => helpEl().replaceWith(helpPanes.keyboardHelp), 700);
    });
  });
  if (document.querySelector('main.analyse')) return; // keyboard help is triggered by analyse snabbdom
  helpPanes.keyboardHelp.querySelector('button')!.addEventListener('click', () =>
    domDialog({
      class: 'help.keyboard-help',
      htmlUrl: '/analysis/help',
      easyClose: 'clickOutside',
      modal: true,
      show: true,
    }),
  );
}

function defaultToggleHtml(ctrl: SettingsCtrl, key: SettingKey) {
  const setting = settings[key];
  const label = setting.helpHtml
    ? isTouchDevice()
      ? `<button class="help-button" data-key="${key}" data-icon="${licon.InfoCircle}">${setting.label}</button>`
      : `<span class="hover-help" data-key="${key}">${setting.label}</span>`
    : setting.label;
  return $html`
    <span class="setting">
      ${label}
      <span class="form-check__input">
        <input data-key="${key}" id="${key}" type="checkbox" ${ctrl[key] ? 'checked' : ''}>
        <label class="form-check__label" for="${key}"></label>
      </span>
    </span>`;
}

function defaultToggleListener(e: Event, ctrl: SettingsCtrl, key: SettingKey) {
  ctrl.set(key, (e.target as HTMLInputElement).checked);
}

function helpHtml() {
  const settingShortcutsHtml = Object.values(settings)
    .filter(opt => opt.shortcutHtml)
    .map(opt => `<div class="setting inert">${opt.label}<span>${opt.shortcutHtml}</span></div>`)
    .join('');
  return $html`
    <div class="help-container">
      <fieldset class="help-pane" data-key="keyboardShortcuts">
        <legend>${i18n.site.keyboardShortcuts}</legend>
        <div class="setting inert">${i18n.site.flipBoard}<kbd>f</kbd></div>
        <div class="setting inert">Toggle local engine<kbd>l</kbd></div>
        ${settingShortcutsHtml}
        <button class="button button-empty button-dim show-all">Show all</button>
      </fieldset>
    </div>
    <div class="hover-hint">${i18n.preferences.hoverCheckboxLabelsForHelp}</div>`;
}

function videoHtml(path: string) {
  return $html`
    <video autoplay loop muted playsinline preload="auto">
      <source src="${site.asset.url('video/' + path + '.webm')}" type="video/webm">
    </video>`;
}

function imageHtml(path: string) {
  return `<img src="${site.asset.url('images/help/' + path + '.webp')}" alt="">`;
}
