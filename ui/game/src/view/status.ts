import type { Ctrl } from '../interfaces';

export function bishopOnColor(expandedFen: string, offset: 0 | 1): boolean {
  if (expandedFen.length !== 64) throw new Error('Expanded FEN expected to be 64 characters');

  for (let row = 0; row < 8; row++) {
    for (let col = row % 2 === offset ? 0 : 1; col < 8; col += 2) {
      if (/[bB]/.test(expandedFen[row * 8 + col])) return true;
    }
  }
  return false;
}

export function expandFen(fullFen: FEN): string {
  return fullFen
    .split(' ')[0]
    .replace(/\d/g, n => '1'.repeat(+n))
    .replace(/\//g, '');
}

export function insufficientMaterial(variant: VariantKey, fullFen: FEN): boolean {
  // TODO: atomic, antichess, threeCheck
  if (
    variant === 'horde' ||
    variant === 'kingOfTheHill' ||
    variant === 'racingKings' ||
    variant === 'crazyhouse' ||
    variant === 'atomic' ||
    variant === 'antichess' ||
    variant === 'threeCheck'
  )
    return false;
  const pieces = fullFen.split(' ')[0].replace(/[^a-z]/gi, '');
  if (/^[Kk]{2}$/.test(pieces)) return true;
  if (/[prq]/i.test(pieces)) return false;
  if (/^[KkNn]{3}$/.test(pieces)) return true;
  if (/b/i.test(pieces)) {
    const expandedFen = expandFen(fullFen);
    return (!bishopOnColor(expandedFen, 0) || !bishopOnColor(expandedFen, 1)) && !/[pPnN]/.test(pieces);
  }
  return false;
}

export default function status(ctrl: Ctrl): string {
  const d = ctrl.data,
    winnerSuffix = d.game.winner
      ? ' • ' + i18n.site[d.game.winner === 'white' ? 'whiteIsVictorious' : 'blackIsVictorious']
      : '';
  switch (d.game.status.name) {
    case 'started':
      return i18n.site.playingRightNow;
    case 'aborted':
      return i18n.site.gameAborted + winnerSuffix;
    case 'mate':
      return i18n.site.checkmate + winnerSuffix;
    case 'resign':
      return i18n.site[d.game.winner === 'white' ? 'blackResigned' : 'whiteResigned'] + winnerSuffix;
    case 'stalemate':
      return i18n.site.stalemate + winnerSuffix;
    case 'timeout':
      switch (d.game.winner) {
        case 'white':
          return i18n.site.blackLeftTheGame + winnerSuffix;
        case 'black':
          return i18n.site.whiteLeftTheGame + winnerSuffix;
        default:
          return `${d.game.turns % 2 === 0 ? i18n.site.whiteLeftTheGame : i18n.site.blackLeftTheGame} • ${i18n.site.draw}`;
      }
    case 'draw': {
      if (d.game.fiftyMoves || d.game.fen.split(' ')[4] === '100')
        return `${i18n.site.fiftyMovesWithoutProgress} • ${i18n.site.draw}`;
      if (d.game.threefold) return `${i18n.site.threefoldRepetition} • ${i18n.site.draw}`;
      if (insufficientMaterial(d.game.variant.key, d.game.fen))
        return `${i18n.site.insufficientMaterial} • ${i18n.site.draw}`;
      if (d.game.drawOffers?.some(turn => turn >= d.game.turns)) return i18n.site.drawByMutualAgreement;
      return i18n.site.draw;
    }
    case 'outoftime':
      return `${d.game.turns % 2 === 0 ? i18n.site.whiteTimeOut : i18n.site.blackTimeOut}${
        winnerSuffix || ` • ${i18n.site.draw}`
      }`;
    case 'noStart':
      return (d.game.winner === 'white' ? i18n.site.blackDidntMove : i18n.site.whiteDidntMove) + winnerSuffix;
    case 'cheat':
      return i18n.site.cheatDetected + winnerSuffix;
    case 'variantEnd':
      switch (d.game.variant.key) {
        case 'kingOfTheHill':
          return i18n.site.kingInTheCenter + winnerSuffix;
        case 'threeCheck':
          return i18n.site.threeChecks + winnerSuffix;
      }
      return i18n.site.variantEnding + winnerSuffix;
    case 'unknownFinish':
      return d.game.winner
        ? i18n.site[d.game.winner === 'white' ? 'whiteIsVictorious' : 'blackIsVictorious']
        : i18n.site.finished;
    default:
      return d.game.status.name + winnerSuffix;
  }
}
