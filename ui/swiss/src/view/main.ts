import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { onInsert } from './util';
import SwissController from '../ctrl';
import * as pagination from '../pagination';
import { MaybeVNodes } from '../interfaces';
import header from './header';
import standing from './standing';
import * as button from './button';

export default function(ctrl: SwissController) {
  const d = ctrl.data;
  const content = (d.status == 'created' ? created(ctrl) : created(ctrl));
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

function created(ctrl: SwissController): MaybeVNodes {
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

function controls(ctrl: SwissController, pag): VNode {
  return h('div.swiss__controls', [
    h('div.pager', pagination.renderPager(ctrl, pag)),
    button.joinWithdraw(ctrl)
  ]);
}
