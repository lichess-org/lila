import type { Redraw, MaybeVNode } from 'common/snabbdom';
import { DasherCtrl } from './ctrl';
import { json as xhrJson, text as xhrText } from 'common/xhr';
import { spinnerVdom, spinnerHtml } from 'common/spinner';
import { init as initSnabbdom, type VNode, classModule, attributesModule, h } from 'snabbdom';
import { frag } from 'common';

const patch = initSnabbdom([classModule, attributesModule]);

let pollsElPromise: Promise<HTMLElement>;
let lastBoard: string;
let lastPieces: string;

export function load(): Promise<DasherCtrl> {
  return site.asset.loadEsm<DasherCtrl>('dasher');
}

export default async function initModule(): Promise<DasherCtrl> {
  let vnode: VNode,
    ctrl: DasherCtrl | undefined = undefined;

  const $el = $('#dasher_app').html(`<div class="initiating">${spinnerHtml}</div>`);
  const element = $el.empty()[0] as HTMLElement;

  const redraw: Redraw = () => {
    vnode = patch(
      vnode || element,
      h('div#dasher_app.dropdown', ctrl?.render() ?? h('div.initiating', spinnerVdom())),
    );
  };

  redraw();

  const data = await xhrJson('/dasher');
  ctrl = new DasherCtrl(data, redraw);
  redraw();

  return ctrl;
}

function board(): string {
  return document.querySelector('#main-wrap')?.classList.contains('is3d')
    ? document.body.dataset.board3d!
    : document.body.dataset.board!;
}

function pieceSet(): string {
  return document.querySelector('#main-wrap')?.classList.contains('is3d')
    ? document.body.dataset.pieceSet3d!
    : document.body.dataset.pieceSet!;
}

async function loadAsk(boards: any, pieceSets: any): Promise<any> {
  lastBoard = board();
  lastPieces = pieceSet();
  const bid = boards.find((b: any) => b._id === lastBoard).id;
  const pid = pieceSets.find((p: any) => p._id === lastPieces).id;
  if (!bid || !pid) return frag<HTMLDivElement>('<div></div>');
  const [bask, pask] = await Promise.all([bid, pid].map(id => xhrText('/ask/' + id)));

  return frag<HTMLDivElement>(`<div>
    <div class="ask-container">${bask}</div>
    <div class="ask-container">${pask}</div>
    </div>`);
}

export function dasherPolls(boards: any, pieces: any /*, redraw: () => void*/): MaybeVNode {
  if (!boards || !pieces) return undefined;
  const newBoard = board();
  const newPieces = pieceSet();
  if (lastBoard !== newBoard || lastPieces !== newPieces) {
    pollsElPromise = loadAsk(boards, pieces);
  }
  console.log('heyo!');
  return h('div#dasher-polls', {
    key: `${newBoard}-${newPieces}`,
    hook: {
      insert: async v => {
        if (!(v.elm instanceof HTMLElement)) return;
        v.elm.append(await pollsElPromise);
        await Promise.all([site.asset.loadEsm('bits.ask', { init: {} }), site.asset.loadCssPath('bits.ask')]);
        v;
        console.log('inserted');
      },
    },
  });
}
