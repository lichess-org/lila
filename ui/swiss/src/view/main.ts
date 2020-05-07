import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { spinner, dataIcon, bind, onInsert } from './util';
import SwissCtrl from '../ctrl';
import * as pagination from '../pagination';
import { MaybeVNodes, SwissData } from '../interfaces';
import header from './header';
import standing from './standing';
import podium from './podium';
import playerInfo from './playerInfo';

export default function(ctrl: SwissCtrl) {
  const d = ctrl.data;
  const content = (d.status == 'created' ? created(ctrl) : (d.status == 'started' ? started(ctrl) : finished(ctrl)));
  return h('main.' + ctrl.opts.classes, [
    h('aside.swiss__side', {
      hook: onInsert(el => {
        $(el).replaceWith(ctrl.opts.$side);
        ctrl.opts.chat && window.lichess.makeChat(ctrl.opts.chat);
      })
    }),
    h('div.swiss__underchat', {
      hook: onInsert(el => {
        $(el).replaceWith($('.swiss__underchat.none').removeClass('none'));
      })
    }),
    playerInfo(ctrl),
    h('div.swiss__main',
      h('div.box.swiss__main-' + d.status, content)
    ),
    ctrl.opts.chat ? h('div.chat__members.none', [
      h('span.number', '\xa0'), ' ', ctrl.trans.noarg('spectators'), ' ', h('span.list')
    ]) : null
  ]);
}

function created(ctrl: SwissCtrl): MaybeVNodes {
  const pag = pagination.players(ctrl);
  return [
    header(ctrl),
    controls(ctrl, pag),
    standing(ctrl, pag, 'created'),
    h('blockquote.pull-quote', [
      h('p', ctrl.data.quote.text),
      h('footer', ctrl.data.quote.author)
    ])
  ];
}

function started(ctrl: SwissCtrl): MaybeVNodes {
  const gameId = ctrl.data.me?.gameId,
  pag = pagination.players(ctrl);
  return [
    header(ctrl),
    gameId ? joinTheGame(ctrl, gameId) : null,
    controls(ctrl, pag),
    standing(ctrl, pag, 'started'),
  ];
}

function finished(ctrl: SwissCtrl): MaybeVNodes {
  const pag = pagination.players(ctrl);
  return [
    h('div.big_top', [
      confetti(ctrl.data),
      header(ctrl),
      podium(ctrl)
    ]),
    controls(ctrl, pag),
    standing(ctrl, pag, 'finished'),
  ];
}

function controls(ctrl: SwissCtrl, pag): VNode {
  return h('div.swiss__controls', [
    h('div.pager', pagination.renderPager(ctrl, pag)),
    joinButton(ctrl)
  ]);
}

function joinButton(ctrl: SwissCtrl): VNode | undefined {
  const d = ctrl.data;
  if (!ctrl.opts.userId) return h('a.fbt.text.highlight', {
    attrs: {
      href: '/login?referrer=' + window.location.pathname,
      'data-icon': 'G'
    }
  }, ctrl.trans('signIn'));

  if (d.joinTeam) return h('a.fbt.text.highlight', {
    attrs: {
      href: `/team/${d.joinTeam}`,
      'data-icon': 'f'
    }
  }, 'Join the team');

  if (d.canJoin) return ctrl.joinSpinner ? spinner() :
    h('button.fbt.text.highlight', {
      attrs: dataIcon('G'),
      hook: bind('click', ctrl.join, ctrl.redraw)
    }, ctrl.trans.noarg('join'));

  if (d.me && d.status != 'finished') return d.me.absent ? (ctrl.joinSpinner ? spinner() : h('button.fbt.text.highlight', {
      attrs: dataIcon('G'),
      hook: bind('click', ctrl.join, ctrl.redraw)
    }, ctrl.trans.noarg('join'))) :
    (ctrl.joinSpinner ? spinner() : h('button.fbt.text', {
      attrs: dataIcon('b'),
      hook: bind('click', ctrl.withdraw, ctrl.redraw)
    }, ctrl.trans.noarg('withdraw')));
}

function joinTheGame(ctrl: SwissCtrl, gameId: string) {
  return h('a.swiss__ur-playing.button.is.is-after', {
    attrs: { href: '/' + gameId }
  }, [
    ctrl.trans('youArePlaying'), h('br'), ctrl.trans('joinTheGame')
  ]);
}

function confetti(data: SwissData): VNode | undefined {
  if (data.me && data.isRecentlyFinished && window.lichess.once('tournament.end.canvas.' + data.id))
    return h('canvas#confetti', {
      hook: {
        insert: _ => window.lichess.loadScript('javascripts/confetti.js')
      }
    });
}
