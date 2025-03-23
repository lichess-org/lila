import { type LooseVNode, looseH as h, bind } from 'common/snabbdom';
import * as licon from 'common/licon';
import { renderClock } from '../clock/clockView';
import RoundController from '../ctrl';
import renderCorresClock from '../corresClock/corresClockView';
import { ClockCtrl } from '../clock/clockCtrl';
import { moretime } from './button';
import { aborted, finished, TopOrBottom } from 'game';
import { justIcon } from '../util';

export const anyClockView = (ctrl: RoundController, position: TopOrBottom): LooseVNode => {
  const player = ctrl.playerAt(position);
  if (ctrl.clock) return renderClock(ctrl.clock, player.color, position, onTheSide(ctrl));
  else if (ctrl.data.correspondence && ctrl.data.game.turns > 1)
    return renderCorresClock(ctrl.corresClock!, player.color, position, ctrl.data.game.player);
  else return whosTurn(ctrl, player.color, position);
};

const onTheSide = (round: RoundController) => (ctrl: ClockCtrl, color: Color, position: TopOrBottom) => {
  const isPlayer = !round.data.player.spectator && round.data.player.color == color;
  return [
    renderBerserk(ctrl, color, position) || (isPlayer ? goBerserk(ctrl, color) : moretime(round)),
    clockSide(ctrl, color, position),
  ];
};

function whosTurn(ctrl: RoundController, color: Color, position: TopOrBottom) {
  const d = ctrl.data;
  if (finished(d) || aborted(d)) return;
  return h(
    'div.rclock.rclock-turn.rclock-' + position,
    d.game.player === color &&
      h(
        'div.rclock-turn__text',
        d.player.spectator
          ? i18n.site[d.game.player === 'white' ? 'whitePlays' : 'blackPlays']
          : i18n.site[d.game.player === d.player.color ? 'yourTurn' : 'waitingForOpponent'],
      ),
  );
}

const showBerserk = (ctrl: ClockCtrl, color: Color): boolean =>
  ctrl.opts.hasGoneBerserk(color) && !ctrl.opts.bothPlayersHavePlayed() && ctrl.opts.playable();

const renderBerserk = (ctrl: ClockCtrl, color: Color, position: TopOrBottom) =>
  showBerserk(ctrl, color) ? h('div.berserked.' + position, justIcon(licon.Berserk)) : null;

const goBerserk = (ctrl: ClockCtrl, color: Color) =>
  showBerserk(ctrl, color) &&
  h('button.fbt.go-berserk', {
    attrs: { title: 'GO BERSERK! Half the time, no increment, bonus point', 'data-icon': licon.Berserk },
    hook: bind('click', ctrl.opts.goBerserk),
  });

const clockSide = (ctrl: ClockCtrl, color: Color, position: TopOrBottom) =>
  ctrl.opts.tournamentRanks() &&
  !showBerserk(ctrl, color) &&
  h(
    'div.tour-rank.' + position,
    { attrs: { title: 'Current tournament rank' } },
    '#' + ctrl.opts.tournamentRanks()?.[color],
  );
