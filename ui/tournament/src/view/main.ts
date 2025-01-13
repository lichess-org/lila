import { makeChat } from 'chat';
import { type MaybeVNodes, onInsert } from 'common/snabbdom';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type TournamentController from '../ctrl';
import { arrangementView } from './arrangement';
import { joinWithTeamSelector } from './battle';
import { created } from './created';
import { finished } from './finished';
import { organizedArrangementView } from './organized-arrangement';
import { playerManagementView } from './player-manage';
import { started } from './started';

export interface ViewHandler {
  name: string;
  main(ctrl: TournamentController): MaybeVNodes;
  table(ctrl: TournamentController): VNode | undefined;
}

export default function (ctrl: TournamentController): VNode {
  let handler: ViewHandler;
  if (ctrl.data.isFinished) handler = finished;
  else if (ctrl.data.isStarted) handler = started;
  else handler = created;

  return h(`main.${ctrl.data.system}${!ctrl.isArena() ? '.arr-table' : ''}.${ctrl.opts.classes}`, [
    h('aside.tour__side', {
      hook: onInsert(el => {
        $(el).replaceWith(ctrl.opts.$side);
        ctrl.opts.chat && makeChat(ctrl.opts.chat);
      }),
    }),
    h('div.tour__underchat', {
      hook: onInsert(el => {
        $(el).replaceWith($('.tour__underchat.none').removeClass('none'));
      }),
    }),
    handler.table(ctrl),
    h(
      'div.tour__main',
      h(
        `div.box.${handler.name}`,
        {
          class: { 'tour__main-finished': ctrl.data.isFinished },
        },
        ctrl.arrangement
          ? arrangementView(ctrl, ctrl.arrangement)
          : ctrl.playerManagement
            ? playerManagementView(ctrl)
            : ctrl.newArrangement
              ? organizedArrangementView(ctrl)
              : handler.main(ctrl),
      ),
    ),
    ctrl.opts.chat
      ? h('div.chat__members.none', [
          h('span.number', '\xa0'),
          ' ',
          i18n('spectators'),
          ' ',
          h('span.list'),
        ])
      : null,
    ctrl.joinWithTeamSelector ? joinWithTeamSelector(ctrl) : null,
  ]);
}
