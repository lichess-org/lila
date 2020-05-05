import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { spinner, dataIcon, bind, onInsert } from './util';
import SwissCtrl from '../ctrl';
import * as pagination from '../pagination';
import { MaybeVNodes } from '../interfaces';
import header from './header';
import standing from './standing';

export default function(ctrl: SwissCtrl) {
  const d = ctrl.data;
  const content = (d.status == 'created' ? created(ctrl) : started(ctrl));
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

function controls(ctrl: SwissCtrl, pag): VNode {
  return h('div.swiss__controls', [
    h('div.pager', pagination.renderPager(ctrl, pag)),
    joinButton(ctrl)
  ]);
}

function joinButton(ctrl: SwissCtrl): VNode | undefined {
  if (!ctrl.opts.userId) return h('a.fbt.text.highlight', {
    attrs: {
      href: '/login?referrer=' + window.location.pathname,
      'data-icon': 'G'
    }
  }, ctrl.trans('signIn'));

  if (ctrl.data.canJoin) return ctrl.joinSpinner ? spinner() :
    h('button.fbt.text.highlight', {
      attrs: dataIcon('G'),
      hook: bind('click', ctrl.join, ctrl.redraw)
    }, ctrl.trans.noarg('join'));
}

function joinTheGame(ctrl: SwissCtrl, gameId: string) {
  return h('a.swiss__ur-playing.button.is.is-after', {
    attrs: { href: '/' + gameId }
  }, [
    ctrl.trans('youArePlaying'), h('br'), ctrl.trans('joinTheGame')
  ]);
}
