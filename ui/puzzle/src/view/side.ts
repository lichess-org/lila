import { h, thunk } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { dataIcon, innerHTML, puzzleUrl } from '../util';
import { Controller } from '../interfaces';

// useful in translation arguments
function strong(txt) {
  return '<strong>' + txt + '</strong>';
}

function hidden() {
  return '<span class="hidden">[hidden]</span>';
}

export function puzzleBox(ctrl: Controller) {
  var data = ctrl.getData();
  return h('div.puzzle__side__metas', [
    puzzleInfos(ctrl, data.puzzle),
    gameInfos(ctrl, data.game, data.puzzle)
  ]);
}

function puzzleInfos(ctrl: Controller, puzzle): VNode {
  const data = ctrl.getData();
  return h('div.infos.puzzle', {
    attrs: puzzle.variant.key === 'frisian' ? dataIcon('$') : dataIcon('-')
  }, [h('div', [
    h('a.title', {
      attrs: { href: puzzleUrl(data.puzzle.variant.key) + puzzle.id }
    }, ctrl.trans('puzzleId', puzzle.id)),
    h('p', {
      hook: innerHTML(ctrl.trans('ratingX', ctrl.vm.mode === 'play' ? hidden() : strong(puzzle.rating)))
    }),
    h('p', {
      hook: innerHTML(ctrl.trans('playedXTimes', strong(window.lidraughts.numberFormat(puzzle.attempts))))
    })
  ])]);
}

function gameInfos(ctrl: Controller, game, puzzle): VNode | undefined {
  return !game.id ? undefined : h('div.infos', {
      attrs: dataIcon(game.perf.icon)
    }, [h('div', [
      h('p', {
        hook: innerHTML(ctrl.trans('fromGameLink', '<a href="/' + game.id + '/' + puzzle.color + '#' + puzzle.initialPly + '">#' + game.id + '</a>'))
      }),
      h('p', [
        game.clock, ' • ',
        game.perf.name, ' • ',
        ctrl.trans.noarg(game.rated ? 'rated' : 'casual')
    ]),
    h('div.players', game.players.map(function(p) {
      return h('div.player.color-icon.is.text.' + p.color,
        p.userId ? h('a.user_link.ulpt', {
          attrs: { href: '/@/' + p.userId }
        }, p.name) : p.name
      );
    }))
  ])]);
}

export function userBox(ctrl: Controller) {
  const data = ctrl.getData();
  if (!data.user) return;
  let ratingHtml = data.user.rating;
  if (ctrl.vm.round) {
    let diff = ctrl.vm.round.ratingDiff,
      klass = '';
    if (diff >= 0) {
      diff = '+' + diff;
      if (diff > 0) klass = 'up';
    } else if (diff === 0) diff = '+0';
    else {
      diff = '−' + (-diff);
      klass = 'down';
    }
    ratingHtml += ' <span class="rp ' + klass + '">' + diff + '</span>';
  }
  const hash = ctrl.recentHash();
  return h('div.puzzle__side__user', [
    h('h2', {
      hook: innerHTML(ctrl.trans('yourPuzzleRatingX', strong(ratingHtml)))
    }),
    h('div', thunk('div.rating_chart.' + hash, ratingChart, [ctrl, hash]))
  ]);
}

function ratingChart(ctrl: Controller, hash: string) {
  return h('div.rating_chart.' + hash, {
    hook: {
      insert(vnode) { drawRatingChart(ctrl, vnode) },
      postpatch(_, vnode) { drawRatingChart(ctrl, vnode) }
    }
  });
}

function drawRatingChart(ctrl: Controller, vnode: VNode) {
  const $el = $(vnode.elm as HTMLElement);
  const dark = document.body.classList.contains('dark');
  const points = ctrl.getData().user.recent.map(function(r) {
    return r[2] + r[1];
  });
  window.lidraughts.raf(() => {
    console.log($el.outerWidth());
    $el['sparkline'](points, {
      type: 'line',
      width: Math.round($el.outerWidth()) + 'px',
      height: '80px',
      lineColor: dark ? '#4444ff' : '#0000ff',
      fillColor: dark ? '#222255' : '#ccccff',
      numberFormatter: (x: number) => { return x; }
    });
  });
}
