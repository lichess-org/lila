import { storedBooleanProp } from 'lib/storage';
import { url as xhrUrl } from 'lib/xhr';
import * as licon from 'lib/licon';
import type AnalyseCtrl from './ctrl';
import { domDialog, type Dialog } from 'lib/view';
import { opposite } from 'chessops';
import { fixCrazySan, plyToTurn } from 'lib/game/chess';
import { ops as treeOps } from 'lib/tree/tree';

export function initModule(ctrl: AnalyseCtrl): void {
  let gifOrientation: Color = ctrl.bottomColor();

  const gifPrefs = {
    players: {
      label: i18n.site.playerNames,
      prop: storedBooleanProp('analyse.gif.players', true),
    },
    ratings: {
      label: i18n.preferences.showPlayerRatings,
      prop: storedBooleanProp('analyse.gif.ratings', true),
    },
    glyphs: {
      label: i18n.site.moveAnnotations,
      prop: storedBooleanProp('analyse.gif.glyphs', false),
    },
    clocks: {
      label: i18n.preferences.chessClock,
      prop: storedBooleanProp('analyse.gif.clocks', false),
    },
  };

  const moveLabel = ({ ply, san }: { ply: number; san?: string }): string =>
    `${plyToTurn(ply)}${ply & 1 ? '.' : '...'} <san>${fixCrazySan(san!)}</san>`;

  const getRangeLabel = (): string => {
    const mainline = treeOps.mainlineNodeList(ctrl.tree.root);
    const range = ctrl.getRangePlies();
    const start = mainline.find(n => n.ply === (range?.from ?? 1));
    const end = mainline.find(n => n.ply === (range?.to ?? ctrl.tree.lastPly()));
    if (!start?.san || !end?.san) return '';
    return start === end ? moveLabel(start) : `${moveLabel(start)} - ${moveLabel(end)}`;
  };

  const buildGifUrl = () => {
    const ds = document.body.dataset;
    const range = ctrl.getRangePlies();
    return xhrUrl(`${ds.assetUrl}/game/export/gif/${gifOrientation}/${ctrl.data.game.id}.gif`, {
      theme: ds.board,
      piece: ds.pieceSet,
      fromPly: range?.from,
      toPly: range?.to,
      ...Object.fromEntries(Object.entries(gifPrefs).map(([k, { prop }]) => [k, prop()])),
    });
  };

  const makeToggle = (key: keyof typeof gifPrefs) => `
    <div class="setting">
      <div class="switch">
        <input id="gif-${key}" class="cmn-toggle" type="checkbox" ${gifPrefs[key].prop() ? 'checked' : ''}>
        <label for="gif-${key}"></label>
      </div>
      <label for="gif-${key}">${gifPrefs[key].label}</label>
    </div>`;

  const updateUrl = (dlg: Dialog) =>
    ((dlg.view.querySelector('.gif-download') as HTMLAnchorElement).href = buildGifUrl());

  const toggleAction = (key: keyof typeof gifPrefs) => ({
    selector: `#gif-${key}`,
    event: 'change',
    listener: (ev: Event, dlg: Dialog) => {
      gifPrefs[key].prop((ev.target as HTMLInputElement).checked);
      updateUrl(dlg);
    },
  });

  domDialog({
    class: 'gif-export',
    modal: true,
    show: true,
    htmlText: `
      <div class="gif-export-dialog">
        <strong style="font-size:1.5em">${i18n.site.gameAsGIF}</strong>
        <div class="gif-options">
          <button class="button button-empty text gif-flip" data-icon="${licon.ChasingArrows}">
            ${i18n.site[gifOrientation]}
          </button>
          <div class="gif-range">${getRangeLabel()}</div>
          ${Object.keys(gifPrefs).map(makeToggle).join('')}
        </div>
        <div class="gif-actions">
          <button class="button button-metal text gif-copy" data-icon="${licon.Clipboard}">
            ${i18n.site.copyToClipboard}
          </button>
          <a class="button button-green text gif-download" data-icon="${licon.Download}" href="${buildGifUrl()}" target="_blank">
            ${i18n.site.download}
          </a>
        </div>
      </div>`,
    actions: [
      {
        selector: '.gif-flip',
        listener: (_, dlg) => {
          gifOrientation = opposite(gifOrientation);
          dlg.view.querySelector('.gif-flip')!.textContent = i18n.site[gifOrientation];
          updateUrl(dlg);
        },
      },
      {
        selector: '.gif-copy',
        listener: (_, dlg) => {
          const url = dlg.view.querySelector<HTMLAnchorElement>('.gif-download')!.href;
          navigator.clipboard.writeText(url).then(() => {
            const btn = dlg.view.querySelector<HTMLButtonElement>('.gif-copy')!;
            btn.dataset.icon = licon.Checkmark;
            btn.classList.remove('button-metal');
            setTimeout(() => {
              btn.dataset.icon = licon.Clipboard;
              btn.classList.add('button-metal');
            }, 1000);
          });
        },
      },
      {
        selector: '.gif-download',
        listener: (_, dlg) => dlg.close(),
      },
      ...Object.keys(gifPrefs).map(toggleAction),
    ],
  });
}
