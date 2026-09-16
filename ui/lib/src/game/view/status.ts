import { opposite } from '@lichess-org/chessground/util';

import { plyColor, type GameData, type Source, type StatusName } from '@/game';

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
    .replace(/\d/g, n => '1'.repeat(Number(n)))
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
    return (!bishopOnColor(expandedFen, 0) || !bishopOnColor(expandedFen, 1)) && !/[nN]/.test(pieces);
  }
  return false;
}

export interface StatusData {
  winner?: Color;
  status: StatusName;
  abortedBy?: Color;
  ply: Ply;
  fen: FEN;
  variant: VariantKey;
  fiftyMoves?: boolean;
  threefold?: boolean;
  drawOffers?: number[];
  source?: Source;
}

export default function status(d: GameData): string {
  return statusOf({
    winner: d.game.winner,
    status: d.game.status.name,
    abortedBy: d.game.abortedBy,
    ply: d.game.turns,
    fen: d.game.fen,
    variant: d.game.variant.key,
    fiftyMoves: d.game.fiftyMoves,
    threefold: d.game.threefold,
    drawOffers: d.game.drawOffers,
    source: d.game.source,
  });
}
export function statusOf(d: StatusData): string {
  const winnerSuffix = d.winner ? ` • ${i18n.site[`${d.winner}IsVictorious`]}` : '';
  switch (d.status) {
    case 'started':
      return i18n.site.playingRightNow;
    case 'aborted':
      const abortReasonText = d.abortedBy
        ? i18n.site[`${d.abortedBy}Aborted`]
        : i18n.site[`${plyColor(d.ply)}DidntMove`];
      return `${abortReasonText}${winnerSuffix}`;
    case 'mate':
      return i18n.site.checkmate + winnerSuffix;
    case 'resign':
      return i18n.site[`${opposite(d.winner ?? 'black')}Resigned`] + winnerSuffix;
    case 'stalemate':
      return i18n.site.stalemate + winnerSuffix;
    case 'timeout':
      return d.winner
        ? i18n.site[`${opposite(d.winner)}LeftTheGame`] + winnerSuffix
        : `${i18n.site[`${plyColor(d.ply)}LeftTheGame`]} • ${i18n.site.draw}`;
    case 'draw': {
      if (d.fiftyMoves || d.fen.split(' ')[4] === '100')
        return `${i18n.site.fiftyMovesWithoutProgress} • ${i18n.site.draw}`;
      if (d.threefold) return `${i18n.site.threefoldRepetition} • ${i18n.site.draw}`;
      if (insufficientMaterial(d.variant, d.fen))
        return `${i18n.site.insufficientMaterial} • ${i18n.site.draw}`;
      if (d.drawOffers?.some(turn => turn >= d.ply)) return i18n.site.drawByMutualAgreement;
      return i18n.site.draw;
    }
    case 'insufficientMaterialClaim':
      return `${i18n.site.drawClaimed} • ${i18n.site.insufficientMaterial}`;
    case 'outoftime':
      return `${i18n.site[`${plyColor(d.ply)}RanOutOfTime`]}${winnerSuffix || ` • ${i18n.site.draw}`}`;
    case 'noStart':
      return i18n.site[`${opposite(d.winner ?? 'black')}DidntMove`] + winnerSuffix;
    case 'cheat':
      return i18n.site.cheatDetected + winnerSuffix;
    case 'variantEnd':
      switch (d.variant) {
        case 'kingOfTheHill':
          return i18n.site.kingInTheCenter + winnerSuffix;
        case 'threeCheck':
          return i18n.site.threeChecks + winnerSuffix;
      }
      return i18n.site.variantEnding + winnerSuffix;
    case 'unknownFinish':
      return d.winner ? i18n.site[`${d.winner}IsVictorious`] : i18n.site.finished;
    default:
      return d.status + winnerSuffix;
  }
}
