import * as cg from '@lichess-org/chessground/types';
import * as util from '@lichess-org/chessground/util';

import { elemAt } from 'lib';

export class Premove {
  readonly unrestrictedPremoves: boolean;

  constructor(
    readonly variant: VariantKey,
    readonly rookCastle: boolean,
  ) {
    this.unrestrictedPremoves = ['atomic', 'crazyhouse'].includes(variant);
  }

  private readonly isDestOccupiedByFriendly = (ctx: cg.MobilityContext): boolean =>
    ctx.friendlies.has(ctx.dest.key);

  private readonly isDestOccupiedByEnemy = (ctx: cg.MobilityContext): boolean =>
    ctx.enemies.has(ctx.dest.key);

  private readonly anyPieceBetween = (orig: cg.Pos, dest: cg.Pos, pieces: cg.Pieces): boolean =>
    util.squaresBetween(...orig, ...dest).some(s => pieces.has(s));

  private readonly canEnemyPawnAdvanceToSquare = (
    pawnStart: cg.Key,
    dest: cg.Key,
    ctx: cg.MobilityContext,
  ): boolean => {
    const piece = ctx.enemies.get(pawnStart);
    if (piece?.role !== 'pawn') return false;
    const step = piece.color === 'white' ? 1 : -1;
    const startPos = util.key2pos(pawnStart);
    const destPos = util.key2pos(dest);
    return (
      util.pawnDirAdvance(...startPos, ...destPos, piece.color === 'white') &&
      !this.anyPieceBetween(startPos, [destPos[0], destPos[1] + step], ctx.allPieces)
    );
  };

  private readonly canEnemyPawnCaptureOnSquare = (
    pawnStart: cg.Key,
    dest: cg.Key,
    ctx: cg.MobilityContext,
  ): boolean => {
    const enemyPawn = ctx.enemies.get(pawnStart);
    return (
      enemyPawn?.role === 'pawn' &&
      util.pawnDirCapture(...util.key2pos(pawnStart), ...util.key2pos(dest), enemyPawn.color === 'white') &&
      (ctx.friendlies.has(dest) ||
        this.canBeCapturedBySomeEnemyEnPassant(
          ctx,
          util.squareShiftedVertically(dest, enemyPawn.color === 'white' ? -1 : 1),
        ))
    );
  };

  private readonly canSomeEnemyPawnAdvanceToDest = (ctx: cg.MobilityContext): boolean =>
    [...ctx.enemies.keys()].some(key => this.canEnemyPawnAdvanceToSquare(key, ctx.dest.key, ctx));

  private readonly isDestControlledByEnemy = (
    ctx: cg.MobilityContext,
    pieceRolesExclude?: cg.Role[],
    specificEnemies?: cg.Pieces,
  ): boolean => {
    const square: cg.Pos = ctx.dest.pos;
    return [...(specificEnemies ?? ctx.enemies)].some(([key, piece]) => {
      const piecePos = util.key2pos(key);
      return (
        !pieceRolesExclude?.includes(piece.role) &&
        ((piece.role === 'pawn' && util.pawnDirCapture(...piecePos, ...square, piece.color === 'white')) ||
          (piece.role === 'knight' && util.knightDir(...piecePos, ...square)) ||
          (piece.role === 'bishop' && util.bishopDir(...piecePos, ...square)) ||
          (piece.role === 'rook' && util.rookDir(...piecePos, ...square)) ||
          (piece.role === 'queen' && util.queenDir(...piecePos, ...square)) ||
          (piece.role === 'king' && util.kingDirNonCastling(...piecePos, ...square))) &&
        (!['bishop', 'rook', 'queen'].includes(piece.role) ||
          !this.anyPieceBetween(piecePos, square, ctx.allPieces))
      );
    });
  };

  private readonly canBeCapturedBySomeEnemyEnPassant = (
    ctx: cg.MobilityContext,
    potentialSquareOfFriendlyPawn: cg.Key | undefined,
    specificEnemies?: cg.Pieces,
    forbiddenEnPassantSquares?: cg.Key[],
  ): boolean => {
    if (!potentialSquareOfFriendlyPawn || (ctx.lastMove && potentialSquareOfFriendlyPawn !== ctx.lastMove[1]))
      return false;
    const pos = util.key2pos(potentialSquareOfFriendlyPawn);
    return (
      ctx.friendlies.get(potentialSquareOfFriendlyPawn)?.role === 'pawn' &&
      pos[1] === (ctx.color === 'white' ? 3 : 4) &&
      (!ctx.lastMove || util.diff(util.key2pos(ctx.lastMove[0])[1], pos[1]) === 2) &&
      [1, -1].some(delta => {
        const k = util.pos2key([pos[0] + delta, pos[1]]);
        return k && (specificEnemies ?? ctx.enemies).get(k)?.role === 'pawn';
      }) &&
      !forbiddenEnPassantSquares?.includes(
        util.squareShiftedVertically(potentialSquareOfFriendlyPawn, ctx.color === 'white' ? -1 : 1)!,
      )
    );
  };

  private readonly isPathClearEnoughForPremove = (
    ctx: cg.MobilityContext,
    isPawnAdvance: boolean,
  ): boolean => {
    if (this.unrestrictedPremoves) return true;
    const squaresBetween = util.squaresBetween(...ctx.orig.pos, ...ctx.dest.pos);
    if (isPawnAdvance) squaresBetween.push(ctx.dest.key);
    const squaresOfFriendliesBetween = squaresBetween.filter(s => ctx.friendlies.has(s));
    const squaresOfEnemiesBetween = squaresBetween.filter(s => ctx.enemies.has(s));
    if (squaresOfEnemiesBetween.length > 1 || squaresOfFriendliesBetween.length > 1) return false;
    const friendlySqBetween = elemAt(squaresOfFriendliesBetween, 0);
    const enemySqBetween = elemAt(squaresOfEnemiesBetween, 0);
    if (enemySqBetween) {
      if (ctx.enemies.get(enemySqBetween)?.role === 'pawn') {
        const enemyStep = ctx.color === 'white' ? -1 : 1;
        const squareAbove = util.squareShiftedVertically(enemySqBetween, enemyStep);
        const enemyPawnDests: cg.Key[] = squareAbove
          ? [
              ...util
                .adjacentSquares(squareAbove)
                .filter(s => this.canEnemyPawnCaptureOnSquare(enemySqBetween, s, ctx)),
              ...[squareAbove, util.squareShiftedVertically(squareAbove, enemyStep)]
                .filter(s => !!s)
                .filter(s => this.canEnemyPawnAdvanceToSquare(enemySqBetween, s, ctx)),
            ]
          : [];
        const badSquares = new Set([...squaresBetween, ctx.orig.key]);
        if (enemyPawnDests.every(square => badSquares.has(square))) return false;
      }
    }
    const enemies = enemySqBetween
      ? new Map([...ctx.enemies].filter(([sq]) => sq === enemySqBetween))
      : ctx.enemies;
    if (!isPawnAdvance && this.isDestOccupiedByFriendly(ctx)) {
      if (friendlySqBetween) return false;
      if (
        !this.isDestControlledByEnemy(ctx, undefined, enemies) &&
        !this.canBeCapturedBySomeEnemyEnPassant(ctx, ctx.dest.key, enemies, squaresBetween)
      )
        return false;
    }
    return (
      !friendlySqBetween ||
      this.canBeCapturedBySomeEnemyEnPassant(ctx, friendlySqBetween, enemies, squaresBetween)
    );
  };

  private readonly pawn: cg.Mobility = (ctx: cg.MobilityContext) => {
    const step = ctx.color === 'white' ? 1 : -1;
    if (util.diff(ctx.orig.pos[0], ctx.dest.pos[0]) > 1) return false;
    if (!util.diff(ctx.orig.pos[0], ctx.dest.pos[0]))
      return (
        util.pawnDirAdvance(...ctx.orig.pos, ...ctx.dest.pos, ctx.color === 'white') &&
        this.isPathClearEnoughForPremove(ctx, true)
      );
    if (ctx.dest.pos[1] !== ctx.orig.pos[1] + step) return false;
    if (this.unrestrictedPremoves || this.isDestOccupiedByEnemy(ctx)) return true;
    if (this.isDestOccupiedByFriendly(ctx)) return this.isDestControlledByEnemy(ctx);
    else
      return (
        this.canSomeEnemyPawnAdvanceToDest(ctx) ||
        this.canBeCapturedBySomeEnemyEnPassant(
          ctx,
          util.pos2key([ctx.dest.pos[0], ctx.dest.pos[1] + step]),
        ) ||
        this.isDestControlledByEnemy(ctx, ['pawn'])
      );
  };

  private readonly king: cg.Mobility = (ctx: cg.MobilityContext) =>
    (util.kingDirNonCastling(...ctx.orig.pos, ...ctx.dest.pos) &&
      (this.unrestrictedPremoves ||
        !this.isDestOccupiedByFriendly(ctx) ||
        this.canBeCapturedBySomeEnemyEnPassant(ctx, ctx.dest.key) ||
        this.isDestControlledByEnemy(ctx))) ||
    (this.variant !== 'antichess' &&
      ctx.orig.pos[1] === ctx.dest.pos[1] &&
      ctx.orig.pos[1] === (ctx.color === 'white' ? 0 : 7) &&
      ((ctx.orig.pos[0] === 4 &&
        this.variant !== 'chess960' &&
        ((ctx.dest.pos[0] === 2 && ctx.rookFilesFriendlies.includes(0)) ||
          (ctx.dest.pos[0] === 6 && ctx.rookFilesFriendlies.includes(7)))) ||
        ((this.rookCastle || this.variant === 'chess960') &&
          ctx.rookFilesFriendlies.includes(ctx.dest.pos[0]))) &&
      (this.unrestrictedPremoves ||
        /* The following checks if no non-rook friendly piece is in the way between the king and its castling destination.
         Note that for the Chess960 edge case of Kb1 "long castling", the check passes even if there is a piece in the way
         on c1. But this is fine, since premoving from b1 to a1 as a normal move would have already returned true. */
        util
          .squaresBetween(...ctx.orig.pos, ctx.dest.pos[0] > ctx.orig.pos[0] ? 7 : 1, ctx.dest.pos[1])
          .map(s => ctx.allPieces.get(s))
          .every(p => !p || util.samePiece(p, { role: 'rook', color: ctx.color }))));

  private readonly basicPieceMobility =
    (dir: (x1: number, y1: number, x2: number, y2: number) => boolean): cg.Mobility =>
    ctx =>
      dir(...ctx.orig.pos, ...ctx.dest.pos) && this.isPathClearEnoughForPremove(ctx, false);

  private readonly mobilityByRole: Record<cg.Role, cg.Mobility> = {
    pawn: this.pawn,
    knight: this.basicPieceMobility(util.knightDir),
    bishop: this.basicPieceMobility(util.bishopDir),
    rook: this.basicPieceMobility(util.rookDir),
    queen: this.basicPieceMobility(util.queenDir),
    king: this.king,
  };

  additionalPremoveRequirements: cg.Mobility = (ctx: cg.MobilityContext) => {
    try {
      return this.mobilityByRole[ctx.role](ctx);
    } catch (e) {
      console.error(e);
      return true;
    }
  };
}
