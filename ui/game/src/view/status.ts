import { Ctrl } from '../interfaces';
import { parseFen } from 'chessops/fen';
import { setupPosition } from 'chessops/variant';

const CHESSOPS_VARIANT_MAP = {
  standard: 'chess',
  chess960: 'chess',
  antichess: 'antichess',
  fromPosition: 'chess',
  kingOfTheHill: 'kingofthehill',
  threeCheck: '3check',
  atomic: 'atomic',
  horde: 'horde',
  racingKings: 'racingkings',
  crazyhouse: 'crazyhouse',
} as const;

export default function status(ctrl: Ctrl): string {
  const noarg = ctrl.trans.noarg,
    d = ctrl.data;
  switch (d.game.status.name) {
    case 'started':
      return noarg('playingRightNow');
    case 'aborted':
      return noarg('gameAborted');
    case 'mate':
      return noarg('checkmate');
    case 'resign':
      return noarg(d.game.winner == 'white' ? 'blackResigned' : 'whiteResigned');
    case 'stalemate':
      return noarg('stalemate');
    case 'timeout':
      switch (d.game.winner) {
        case 'white':
          return noarg('blackLeftTheGame');
        case 'black':
          return noarg('whiteLeftTheGame');
      }
      return `${d.game.turns % 2 === 0 ? noarg('whiteLeftTheGame') : noarg('blackLeftTheGame')}${
        d.game.winner ? '' : ` • ${noarg('draw')}`
      }`;
    case 'draw':
      if (d.game.drawOffers?.some(turn => turn >= d.game.turns)) return noarg('drawByMutualAgreement');
      if (d.game.fen.split(' ')[4] === '100') return `${noarg('fiftyMovesWithoutProgress')} • ${noarg('draw')}`;
      if (d.game.threefold) return `${noarg('threefoldRepetition')} • ${noarg('draw')}`;
      const setup = parseFen(d.game.fen).unwrap();
      const variant = CHESSOPS_VARIANT_MAP[d.game.variant.key];
      const pos = setupPosition(variant, setup).unwrap();
      if (pos.isInsufficientMaterial()) return `${noarg('insufficientMaterial')} • ${noarg('draw')}`;
      return noarg('draw');
    case 'outoftime':
      return `${d.game.turns % 2 === 0 ? noarg('whiteTimeOut') : noarg('blackTimeOut')}${
        d.game.winner ? '' : ` • ${noarg('draw')}`
      }`;
    case 'noStart':
      return (d.game.winner == 'white' ? 'Black' : 'White') + " didn't move";
    case 'cheat':
      return noarg('cheatDetected');
    case 'variantEnd':
      switch (d.game.variant.key) {
        case 'kingOfTheHill':
          return noarg('kingInTheCenter');
        case 'threeCheck':
          return noarg('threeChecks');
      }
      return noarg('variantEnding');
    case 'unknownFinish':
      return 'Finished';
    default:
      return d.game.status.name;
  }
}
