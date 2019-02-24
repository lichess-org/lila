import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Position, MaybeVNodes } from '../interfaces';
import * as game from 'game';
import * as status from 'game/status';
import { renderClock } from '../clock/clockView';
import renderCorresClock from '../corresClock/corresClockView';
import renderReplay from './replay';
import * as renderUser from './user';
import * as button from './button';
import RoundController from '../ctrl';

function renderPlayer(ctrl: RoundController, position: Position) {
  const player = ctrl.playerAt(position);
  return ctrl.nvui ? undefined : (
    player.ai ? h('div.user_link.online.round__app__user-' + position, [
      h('i.line'),
      h('name', renderUser.aiName(ctrl, player.ai))
    ]) :
    renderUser.userHtml(ctrl, player, position)
  );
}

function isLoading(ctrl: RoundController): boolean {
  return ctrl.loading || ctrl.redirecting;
}

function loader() { return h('span.ddloader'); }

function renderTableWith(ctrl: RoundController, buttons: MaybeVNodes) {
  return [
    renderReplay(ctrl),
    h('div.round__app__controls', [
      h('div.control.buttons', buttons)
    ])
  ];
}

function renderTableEnd(ctrl: RoundController) {
  return renderTableWith(ctrl, [
    isLoading(ctrl) ? loader() : (button.backToTournament(ctrl) || button.followUp(ctrl))
  ]);
}

function renderTableWatch(ctrl: RoundController) {
  return renderTableWith(ctrl, [
    isLoading(ctrl) ? loader() : button.watcherFollowUp(ctrl)
  ]);
}

function renderTablePlay(ctrl: RoundController) {
  const d = ctrl.data,
    loading = isLoading(ctrl),
    submit = button.submitMove(ctrl),
    icons = (loading || submit) ? [] : [
      game.abortable(d) ? button.standard(ctrl, undefined, 'L', 'abortGame', 'abort') :
      button.standard(ctrl, game.takebackable, 'i', 'proposeATakeback', 'takeback-yes', ctrl.takebackYes),
      ctrl.drawConfirm ? button.drawConfirm(ctrl) : button.standard(ctrl, ctrl.canOfferDraw, '2', 'offerDraw', 'draw-yes', () => ctrl.offerDraw(true)),
      ctrl.resignConfirm ? button.resignConfirm(ctrl) : button.standard(ctrl, game.resignable, 'b', 'resign', 'resign-confirm', () => ctrl.resign(true))
    ],
    buttons: MaybeVNodes = loading ? [loader()] : (submit ? [submit] : [
      button.forceResign(ctrl),
      button.threefoldClaimDraw(ctrl),
      button.cancelDrawOffer(ctrl),
      button.answerOpponentDrawOffer(ctrl),
      button.cancelTakebackProposition(ctrl),
      button.answerOpponentTakebackProposition(ctrl)
    ]);
  return [
    renderReplay(ctrl),
    h('div.round__app__controls', [
      h('div.control.icons', {
        class: { 'confirm': !!(ctrl.drawConfirm || ctrl.resignConfirm) }
      }, icons),
      h('div.control.buttons', buttons)
    ])
  ];
}

function whosTurn(ctrl: RoundController, color: Color, position: Position) {
  const d = ctrl.data;
  if (status.finished(d) || status.aborted(d)) return;
  return h('div.rclock.rclock-turn.rclock-' + position,
    d.game.player === color ? (
      d.player.spectator ? ctrl.trans(d.game.player + 'Plays') : ctrl.trans(
        d.game.player === d.player.color ? 'yourTurn' : 'waitingForOpponent'
      )
    ) : ''
  );
}

function anyClock(ctrl: RoundController, position: Position) {
  const player = ctrl.playerAt(position);
  if (ctrl.clock) return renderClock(ctrl, player, position);
  else if (ctrl.data.correspondence && ctrl.data.game.turns > 1)
    return renderCorresClock(
      ctrl.corresClock!, ctrl.trans, player.color, position, ctrl.data.game.player
    );
  else return whosTurn(ctrl, player.color, position);
}

export function renderTable(ctrl: RoundController, expiration: [VNode, boolean] | undefined | false): MaybeVNodes {
  return [
    h('div.round__app__table'),
    anyClock(ctrl, 'top'),
    expiration && !expiration[1] ? expiration[0] : null,
    renderPlayer(ctrl, 'top'),
    ...(ctrl.data.player.spectator ? renderTableWatch(ctrl) : (
      game.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
    )),
    renderPlayer(ctrl, 'bottom'),
    expiration && expiration[1] ? expiration[0] : null,
    anyClock(ctrl, 'bottom')
  ];
};
