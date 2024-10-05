import { onInsert, looseH as h, LooseVNodes } from 'common/snabbdom';
import HomeController from '../ctrl';
import { spinnerVdom } from 'common/spinner';
import { initMiniBoards, initMiniBoard } from 'common/miniBoard';
import { frag } from 'common';
import * as xhr from 'common/xhr';
import { NowPlaying } from '../interfaces';
import { timeago } from 'common/i18n';

export function renderYourGamesTab(ctrl: HomeController): LooseVNodes {
  return [
    ctrl.tab.active === 'playing' && ctrl.data.nbNowPlaying > 0 ? renderPlaying(ctrl) : renderRecent(ctrl),
  ];
}

function renderRecent(ctrl: HomeController) {
  if (!ctrl.me) return null;
  return h(
    'div.recent-games',
    {
      hook: onInsert(async el => {
        const rsp = await xhr.text(`/@/${ctrl.me!.username}/all?page=1`);
        const games = frag<HTMLElement>(`<div>${rsp}</div>`).querySelector('.search__result');
        if (!games || !games.firstElementChild?.childElementCount) {
          el.innerHTML = '<h2>Play some games!</h2>';
          return;
        }
        el.innerHTML = games.innerHTML;
        initMiniBoards();
      }),
    },
    spinnerVdom(),
  );
}

function renderPlaying(ctrl: HomeController) {
  return h(
    'div.now-playing',
    ctrl.data.nowPlaying.map(pov =>
      h('a.' + pov.variant.key, { key: `${pov.gameId}${pov.lastMove}`, attrs: { href: '/' + pov.fullId } }, [
        h('span.mini-board.cg-wrap.is2d', {
          attrs: { 'data-state': `${pov.fen},${pov.orientation || pov.color},${pov.lastMove}` },
          hook: { insert: vnode => initMiniBoard(vnode.elm as HTMLElement) },
        }),
        h('span.meta', [
          pov.opponent.ai
            ? ctrl.trans('aiNameLevelAiLevel', 'Stockfish', pov.opponent.ai)
            : pov.opponent.username,
          h(
            'span.indicator',
            pov.isMyTurn
              ? pov.secondsLeft && pov.hasMoved
                ? timer(pov)
                : [ctrl.trans.noarg('yourTurn')]
              : h('span', '\xa0'),
          ), // &nbsp;
        ]),
      ]),
    ),
  );
}

function timer(pov: NowPlaying) {
  const date = Date.now() + pov.secondsLeft! * 1000;
  return h(
    'time.timeago',
    {
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLElement).setAttribute('datetime', '' + date);
        },
      },
    },
    timeago(date),
  );
}
