import { type LooseVNode, hl, bind } from 'lib/snabbdom';
import * as licon from 'lib/licon';
import { renderClock } from 'lib/game/clock/clockView';
import RoundController from '../ctrl';
import renderCorresClock from '../corresClock/corresClockView';
import { moretime } from './button';
import {
  aborted,
  berserkableBy,
  bothPlayersHavePlayed,
  finished,
  playable,
  TopOrBottom,
  TournamentRanks,
} from 'lib/game/game';
import { justIcon } from '../util';

export const anyClockView = (ctrl: RoundController, position: TopOrBottom): LooseVNode => {
  const player = ctrl.playerAt(position);
  if (ctrl.clock) return renderClock(ctrl.clock, player.color, position, onTheSide(ctrl));
  else if (ctrl.data.correspondence && ctrl.data.game.turns > 1)
    return renderCorresClock(ctrl.corresClock!, player.color, position, ctrl.data.game.player);
  else return whosTurn(ctrl, player.color, position);
};

const onTheSide = (round: RoundController) => (color: Color, position: TopOrBottom) => {
  const isPlayer = !round.data.player.spectator && round.data.player.color == color;
  const ranks = round.data.tournament?.ranks || round.data.swiss?.ranks;
  return [
    renderBerserk(round, color, position) || (isPlayer ? goBerserk(round, color) : moretime(round)),
    clockSide(round, color, position, ranks),
  ];
};

function whosTurn(ctrl: RoundController, color: Color, position: TopOrBottom) {
  const d = ctrl.data;
  if (finished(d) || aborted(d)) return;
  return hl(
    'div.rclock.rclock-turn.rclock-' + position,
    d.game.player === color &&
      hl(
        'div.rclock-turn__text',
        d.player.spectator
          ? i18n.site[d.game.player === 'white' ? 'whitePlays' : 'blackPlays']
          : i18n.site[d.game.player === d.player.color ? 'yourTurn' : 'waitingForOpponent'],
      ),
  );
}

const showBerserk = (ctrl: RoundController, color: Color): boolean =>
  ctrl.hasGoneBerserk(color) && !bothPlayersHavePlayed(ctrl.data) && playable(ctrl.data);

const renderBerserk = (ctrl: RoundController, color: Color, position: TopOrBottom) =>
  showBerserk(ctrl, color) ? hl('div.berserked.' + position, justIcon(licon.Berserk)) : null;

const goBerserk = (ctrl: RoundController, color: Color) =>
  berserkableBy(ctrl.data) &&
  !ctrl.hasGoneBerserk(color) &&
  hl('button.fbt.go-berserk', {
    attrs: { title: 'GO BERSERK! Half the time, no increment, bonus point', 'data-icon': licon.Berserk },
    hook: bind('click', ctrl.goBerserk),
  });

const clockSide = (
  ctrl: RoundController,
  color: Color,
  position: TopOrBottom,
  ranks: TournamentRanks | undefined,
) =>
  ranks &&
  !showBerserk(ctrl, color) &&
  hl('div.tour-rank.' + position, { attrs: { title: 'Current tournament rank' } }, '#' + ranks[color]);
