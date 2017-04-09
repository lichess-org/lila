import { AnalyseController } from './interfaces';

import * as m from 'mithril';
import { GameData, game } from 'game';

function renderRatingDiff(rd: number): Mithril.Renderable {
  if (rd === 0) return m('span.rp.null', '±0');
  if (rd > 0) return m('span.rp.up', '+' + rd);
  return m('span.rp.down', rd);
}

function renderPlayer(data: GameData, color: Color): Mithril.Renderable {
  var p = game.getPlayer(data, color);
  if (p.name) return p.name;
  if (p.ai) return 'Stockfish level ' + p.ai;
  if (p.user) return m('a.user_link.ulpt', {
    href: '/@/' + p.user.username
  }, [p.user.username, renderRatingDiff(p.ratingDiff!)]);
  return 'Anonymous';
}

var advices = [
  ['inaccuracy', 'inaccuracies', '?!'],
  ['mistake', 'mistakes', '?'],
  ['blunder', 'blunders', '??']
];

function playerTable(ctrl: AnalyseController, color: Color): Mithril.Renderable {
  var d = ctrl.data;
  return m('table', [
    m('thead', m('tr', [
      m('td', m('i.is.color-icon.' + color)),
      m('th', renderPlayer(d, color))
    ])),
    m('tbody', [
      advices.map(function(a) {
        var nb: number = d.analysis![color][a[0]];
        var attrs = nb ? {
          class: 'symbol',
          'data-color': color,
          'data-symbol': a[2]
        } : {};
        return m('tr', attrs, [
          m('td', nb),
          m('th', ctrl.trans(a[1]))
        ]);
      }),
      m('tr', [
        m('td', d.analysis![color].acpl),
        m('th', ctrl.trans('averageCentipawnLoss'))
      ])
    ])
  ])
}

var cached = false;

export function uncache(): void {
  cached = false;
}

export function render(ctrl: AnalyseController): Mithril.Renderable {
  var d = ctrl.data;
  if (!d.analysis) return;
  if (!ctrl.vm.showComputer()) {
    if (cached) cached = false;
    return;
  }

  var first = ctrl.vm.mainline[0].eval || {};
  if (first.cp || first.mate) {
    if (cached) return {
      subtree: 'retain'
    };
    else cached = true;
  }

  return m('div.advice_summary', {
    config: function(el, isUpdate) {
      if (!isUpdate)
        $(el).on('click', 'tr.symbol', function(this: Element) {
          ctrl.jumpToGlyphSymbol($(this).data('color'), $(this).data('symbol'));
        });
    }
  }, [
    playerTable(ctrl, 'white'),
    m('a', {
      class: 'button text' + (ctrl.retro ? ' active' : ''),
      'data-icon': 'G',
      onclick: ctrl.toggleRetro,
    }, 'Learn from your mistakes'),
    playerTable(ctrl, 'black')
  ]);
}
