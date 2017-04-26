import { game, status }  from 'game';
import clockView = require('../clock/view');
import corresClockView from '../correspondenceClock/view';
import replay = require('./replay');
import renderUser = require('./user');
import button = require('./button');

import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { MaybeVNodes } from '../interfaces';

function playerAt(ctrl, position) {
  return ctrl.vm.flip ^ ((position === 'top') as any) ? ctrl.data.opponent : ctrl.data.player;
}

function topPlayer(ctrl) {
  return playerAt(ctrl, 'top');
}

function bottomPlayer(ctrl) {
  return playerAt(ctrl, 'bottom');
}

function renderPlayer(ctrl, player) {
  return player.ai ? h('div.username.user_link.online', [
    h('i.line'),
    h('name', renderUser.aiName(ctrl, player))
  ]) :
  renderUser.userHtml(ctrl, player);
}

function isLoading(ctrl) {
  return ctrl.vm.loading || ctrl.vm.redirecting;
}

function loader() { return h('span.ddloader'); }

function renderTableWith(ctrl, buttons: MaybeVNodes) {
  return [
    replay.render(ctrl),
    h('div.control.buttons', buttons),
    renderPlayer(ctrl, bottomPlayer(ctrl))
  ];
}

function renderTableEnd(ctrl) {
  return renderTableWith(ctrl, [
    isLoading(ctrl) ? loader() : (button.backToTournament(ctrl) || button.followUp(ctrl))
  ]);
}

function renderTableWatch(ctrl) {
  return renderTableWith(ctrl, [
    isLoading(ctrl) ? loader() : button.watcherFollowUp(ctrl)
  ]);
}

function renderTablePlay(ctrl) {
  const d = ctrl.data;
  let onlyButton = isLoading(ctrl) ? loader() : button.submitMove(ctrl);
  let buttons: MaybeVNodes = onlyButton ? [onlyButton] : [
    button.forceResign(ctrl),
    button.threefoldClaimDraw(ctrl),
    button.cancelDrawOffer(ctrl),
    button.answerOpponentDrawOffer(ctrl),
    button.cancelTakebackProposition(ctrl),
    button.answerOpponentTakebackProposition(ctrl),
    (d.tournament && game.nbMoves(d, d.player.color) === 0) ? h('div.suggestion', [
      h('div.text', { attrs: {'data-icon': 'j'} },
        ctrl.trans('youHaveNbSecondsToMakeYourFirstMove', d.tournament.nbSecondsForFirstMove))
    ]) : null
  ];
  return [
    replay.render(ctrl), (ctrl.vm.moveToSubmit || ctrl.vm.dropToSubmit) ? null : (
      isLoading(ctrl) ? null : h('div.control.icons', [
        game.abortable(d) ? button.standard(ctrl, null, 'L', 'abortGame', 'abort', null) :
        button.standard(ctrl, game.takebackable, 'i', 'proposeATakeback', 'takeback-yes', ctrl.takebackYes),
        button.standard(ctrl, ctrl.canOfferDraw, '2', 'offerDraw', 'draw-yes', ctrl.offerDraw),
        ctrl.vm.resignConfirm ? button.resignConfirm(ctrl) : button.standard(ctrl, game.resignable, 'b', 'resign', 'resign-confirm', ctrl.resign)
      ])
    ),
    h('div.control.buttons', buttons),
    renderPlayer(ctrl, bottomPlayer(ctrl))
  ];
}

function whosTurn(ctrl, color) {
  var d = ctrl.data;
  if (status.finished(d) || status.aborted(d)) return;
  return h('div.whos_turn',
    d.game.player === color ? (
      d.player.spectator ? ctrl.trans(d.game.player + 'Plays') : ctrl.trans(
        d.game.player === d.player.color ? 'yourTurn' : 'waitingForOpponent'
      )
    ) : ''
  );
}

function anyClock(ctrl, position) {
  var player = playerAt(ctrl, position);
  if (ctrl.clock) return clockView.renderClock(ctrl, player, position);
  else if (ctrl.data.correspondence && ctrl.data.game.turns > 1)
  return corresClockView(
    ctrl.correspondenceClock, ctrl.trans, player.color, position, ctrl.data.game.player
  );
  else return whosTurn(ctrl, player.color);
}

export function render(ctrl: any): VNode {
  return h('div.table_wrap', [
    anyClock(ctrl, 'top'),
    h('div.table', [
      renderPlayer(ctrl, topPlayer(ctrl)),
      h('div.table_inner',
        ctrl.data.player.spectator ? renderTableWatch(ctrl) : (
          game.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
        )
      )
    ]),
    anyClock(ctrl, 'bottom')
  ]);
};
