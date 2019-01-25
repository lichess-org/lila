import { h, thunk } from 'snabbdom'
import { VNode, VNodeData } from 'snabbdom/vnode'
import { MaybeVNode } from './interfaces';
import AnalyseCtrl from './ctrl';
import { findTag } from './study/studyChapters';
import * as game from 'game';
import { defined } from 'common';
import { bind, dataIcon } from './util';

function renderRatingDiff(rd: number | undefined): VNode | undefined {
  if (rd === 0) return h('span.rp.null', '±0');
  if (rd && rd > 0) return h('span.rp.up', '+' + rd);
  if (rd && rd < 0) return h('span.rp.down', '−' + (-rd));
  return;
}

function renderPlayer(ctrl: AnalyseCtrl, color: Color): VNode {
  const p = game.getPlayer(ctrl.data, color);
  if (p.user) return h('a.user_link.ulpt', {
    attrs: { href: '/@/' + p.user.username }
  }, [
    h('span', p.user.username),
    renderRatingDiff(p.ratingDiff)
  ]);
  return h('span',
    p.name ||
    (p.ai && 'Stockfish level ' + p.ai) ||
    (ctrl.study && findTag(ctrl.study.data.chapter.tags, color)) ||
    'Anonymous');
}

const advices = [
  ['inaccuracy', 'inaccuracies', '?!'],
  ['mistake', 'mistakes', '?'],
  ['blunder', 'blunders', '??']
];

function playerTable(ctrl: AnalyseCtrl, color: Color): VNode {
  const d = ctrl.data, trans = ctrl.trans.noarg;
  const acpl = d.analysis![color].acpl;
  return h('table', {
    hook: {
      insert(vnode) {
        window.lichess.powertip.manualUserIn(vnode.elm);
      }
    }
  }, [
    h('thead', h('tr', [
      h('td', h('i.is.color-icon.' + color)),
      h('th', renderPlayer(ctrl, color))
    ])),
    h('tbody',
      advices.map(a => {
        const nb: number = d.analysis![color][a[0]];
        const attrs: VNodeData = nb ? {
          'data-color': color,
          'data-symbol': a[2]
        } : {};
        return h('tr' + (nb ? '.symbol' : ''), { attrs }, [
          h('td', '' + nb),
          h('th', trans(a[1]))
        ]);
      }).concat(
        h('tr', [
          h('td', '' + (defined(acpl) ? acpl : '?')),
          h('th', trans('averageCentipawnLoss'))
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
    ctrl.study ? null : h('a.button.text', {
      class: { active: !!ctrl.retro },
      attrs: dataIcon('G'),
      hook: bind('click', ctrl.toggleRetro, ctrl.redraw)
    }, ctrl.trans.noarg('learnFromYourMistakes')),
    playerTable(ctrl, 'black')
  ]);
}

export function render(ctrl: AnalyseCtrl): MaybeVNode {

  if (!ctrl.data.analysis || !ctrl.showComputer() || (ctrl.study && ctrl.study.vm.toolTab() !== 'serverEval')) return;

  // don't cache until the analysis is complete!
  const buster = ctrl.data.analysis.partial ? Math.random() : '';
  let cacheKey = '' + buster + !!ctrl.retro;
  if (ctrl.study) cacheKey += ctrl.study.data.chapter.id;

  return thunk('div.advice_summary', doRender, [ctrl, cacheKey]);
}
