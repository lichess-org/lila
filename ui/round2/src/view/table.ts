import { game, status }  from 'game';
import clockView = require('../clock/view');
import renderCorrespondenceClock = require('../correspondenceClock/view');
import replay = require('./replay');
import renderUser = require('./user');
import button = require('./button');

import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

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
  return player.ai ? h('div', {
    class: {
      username: true,
      user_link: true,
      online: true
    }
  }, [
    h('i', { class: { line: true }}),
    h('name', renderUser.aiName(ctrl, player))
  ]) :
  renderUser.userHtml(ctrl, player);
}

function isLoading(ctrl) {
  return ctrl.vm.loading || ctrl.vm.redirecting;
}

function loader() { return h('span.ddloader'); }

function renderTableWith(ctrl, buttons: VNode[]): VNode[] {
  return [
    replay.render(ctrl),
    buttons ? h('div', {
      class: { control: true, buttons: true }
    }, buttons) : null,
    renderPlayer(ctrl, bottomPlayer(ctrl))
  ] as VNode[];
}

function renderTableEnd(ctrl): VNode[] {
  return renderTableWith(ctrl, [
    isLoading(ctrl) ? loader() : (button.backToTournament(ctrl) || button.followUp(ctrl))
  ]) as VNode[];
}

function renderTableWatch(ctrl): VNode[] {
  return renderTableWith(ctrl, [
    isLoading(ctrl) ? loader() : button.watcherFollowUp(ctrl)
  ]);
}

function renderTablePlay(ctrl) {
  const d = ctrl.data;
  let onlyButton = isLoading(ctrl) ? loader() : button.submitMove(ctrl);
  let buttons = onlyButton ? [onlyButton] : [
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
  ] as VNode[];
  return [
    replay.render(ctrl), (ctrl.vm.moveToSubmit || ctrl.vm.dropToSubmit) ? null : (
      isLoading(ctrl) ? null : h('div', {
        class: {control: true, icons: true}
      }, [
        game.abortable(d) ? button.standard(ctrl, null, 'L', 'abortGame', 'abort', null) :
        button.standard(ctrl, game.takebackable, 'i', 'proposeATakeback', 'takeback-yes', ctrl.takebackYes),
        button.standard(ctrl, ctrl.canOfferDraw, '2', 'offerDraw', 'draw-yes', ctrl.offerDraw),
        ctrl.vm.resignConfirm ? button.resignConfirm(ctrl) : button.standard(ctrl, game.resignable, 'b', 'resign', 'resign-confirm', ctrl.resign)
      ])
    ),
    h('div', {
      class: {control: true, buttons: true}
    }, buttons),
    renderPlayer(ctrl, bottomPlayer(ctrl))
  ] as VNode[];
}

function whosTurn(ctrl, color) {
  var d = ctrl.data;
  if (status.finished(d) || status.aborted(d)) return;
  return h('div', {
    class: {whos_turn: true}
  },
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
  return renderCorrespondenceClock(
    ctrl.correspondenceClock, ctrl.trans, player.color, position, ctrl.data.game.player
  );
  else return whosTurn(ctrl, player.color);
}

export function render(ctrl: any): VNode {
  return h('div', {
    class: {table_wrap: true}
  }, [
    anyClock(ctrl, 'top'),
    h('div', {
      class: {table: true}
    }, [
      renderPlayer(ctrl, topPlayer(ctrl)),
      h('div', {
        class: {table_inner: true}
      },
      ctrl.data.player.spectator ? renderTableWatch(ctrl) : (
        game.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
      )
      )
    ] as VNode[]),
    anyClock(ctrl, 'bottom')
  ] as VNode[]);
};
