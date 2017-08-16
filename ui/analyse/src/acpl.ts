import { h, thunk } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { AnalyseData, MaybeVNode } from './interfaces';
import AnalyseCtrl from './ctrl';
import { game } from 'game';
import { bind, dataIcon } from './util';

function renderRatingDiff(rd: number | undefined): VNode | undefined {
  if (rd === 0) return h('span.rp.null', '±0');
  if (rd && rd > 0) return h('span.rp.up', '+' + rd);
  if (rd && rd < 0) return h('span.rp.down', '' + rd);
  return;
}

function renderPlayer(data: AnalyseData, color: Color): VNode {
  const p = game.getPlayer(data, color);
  if (p.user) return h('a.user_link.ulpt', {
    attrs: { href: '/@/' + p.user.username }
  }, [
    h('span', p.user.username),
    renderRatingDiff(p.ratingDiff)
  ]);
  return h('span', p.name || (p.ai ? 'Stockfish level ' + p.ai : 'Anonymous'));
}

const advices = [
  ['inaccuracy', 'inaccuracies', '?!'],
  ['mistake', 'mistakes', '?'],
  ['blunder', 'blunders', '??']
];

function playerTable(ctrl: AnalyseCtrl, color: Color): VNode {
  const d = ctrl.data;
  return h('table', [
    h('thead', h('tr', [
      h('td', h('i.is.color-icon.' + color)),
      h('th', renderPlayer(d, color))
    ])),
    h('tbody',
      advices.map(a => {
        const nb: number = d.analysis![color][a[0]];
        const attrs = nb ? {
          'data-color': color,
          'data-symbol': a[2]
        } : {};
        return h('tr' + (nb ? '.symbol' : ''), { attrs }, [
          h('td', '' + nb),
          h('th', ctrl.trans(a[1]))
        ]);
      }).concat(
        h('tr', [
          h('td', '' + d.analysis![color].acpl),
          h('th', ctrl.trans('averageCentipawnLoss'))
        ])
      ))
  ])
}

function doRender(ctrl: AnalyseCtrl): VNode {
  return h('div.advice_summary', {
    hook: {
      insert: vnode => {
        $(vnode.elm as HTMLElement).on('click', 'tr.symbol', function(this: Element) {
          ctrl.jumpToGlyphSymbol($(this).data('color'), $(this).data('symbol'));
        });
      }
    }
  }, [
    playerTable(ctrl, 'white'),
    h('a.button.text', {
      class: { active: !!ctrl.retro },
      attrs: dataIcon('G'),
      hook: bind('click', ctrl.toggleRetro, ctrl.redraw)
    }, 'Learn from your mistakes'),
    playerTable(ctrl, 'black')
  ]);
}

export function render(ctrl: AnalyseCtrl): MaybeVNode {

  if (!ctrl.data.analysis || !ctrl.showComputer()) return;

  // don't cache until the analysis is complete!
  const firstEval = ctrl.mainline[0].eval;
  const firstKey = firstEval ? firstEval.cp : Math.random();
  const cacheKey = '' + firstKey + !!ctrl.retro;

  return thunk('div.advice_summary', doRender, [ctrl, cacheKey]);
}
