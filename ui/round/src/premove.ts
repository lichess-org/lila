import * as util from '@lichess-org/chessground/util';
import * as cg from '@lichess-org/chessground/types';

const isDestOccupiedByFriendly = (ctx: cg.MobilityContext): boolean => ctx.friendlies.has(ctx.dest.key);

const isDestOccupiedByEnemy = (ctx: cg.MobilityContext): boolean => ctx.enemies.has(ctx.dest.key);

const anyPieceBetween = (orig: cg.Pos, dest: cg.Pos, pieces: cg.Pieces): boolean =>
  util.squaresBetween(...orig, ...dest).some(s => pieces.has(s));

const canEnemyPawnAdvanceToSquare = (pawnStart: cg.Key, dest: cg.Key, ctx: cg.MobilityContext): boolean => {
  const piece = ctx.enemies.get(pawnStart);
  if (piece?.role !== 'pawn') return false;
  const step = piece.color === 'white' ? 1 : -1;
  const startPos = util.key2pos(pawnStart);
  const destPos = util.key2pos(dest);
  return (
    util.pawnDirAdvance(...startPos, ...destPos, piece.color === 'white') &&
    !anyPieceBetween(startPos, [destPos[0], destPos[1] + step], ctx.allPieces)
  );
};

const canEnemyPawnCaptureOnSquare = (pawnStart: cg.Key, dest: cg.Key, ctx: cg.MobilityContext): boolean => {
  const enemyPawn = ctx.enemies.get(pawnStart);
  return (
    enemyPawn?.role === 'pawn' &&
    util.pawnDirCapture(...util.key2pos(pawnStart), ...util.key2pos(dest), enemyPawn.color === 'white') &&
    (ctx.friendlies.has(dest) ||
      canBeCapturedBySomeEnemyEnPassant(
        util.squareShiftedVertically(dest, enemyPawn.color === 'white' ? -1 : 1),
        ctx.friendlies,
        ctx.enemies,
        ctx.lastMove,
      ))
  );
};

const canSomeEnemyPawnAdvanceToDest = (ctx: cg.MobilityContext): boolean =>
  [...ctx.enemies.keys()].some(key => canEnemyPawnAdvanceToSquare(key, ctx.dest.key, ctx));

const isDestControlledByEnemy = (ctx: cg.MobilityContext, pieceRolesExclude?: cg.Role[]): boolean => {
  const square: cg.Pos = ctx.dest.pos;
  return [...ctx.enemies].some(([key, piece]) => {
    const piecePos = util.key2pos(key);
    return (
      !pieceRolesExclude?.includes(piece.role) &&
      ((piece.role === 'pawn' && util.pawnDirCapture(...piecePos, ...square, piece.color === 'white')) ||
        (piece.role === 'knight' && util.knightDir(...piecePos, ...square)) ||
        (piece.role === 'bishop' && util.bishopDir(...piecePos, ...square)) ||
        (piece.role === 'rook' && util.rookDir(...piecePos, ...square)) ||
        (piece.role === 'queen' && util.queenDir(...piecePos, ...square)) ||
        (piece.role === 'king' && util.kingDirNonCastling(...piecePos, ...square))) &&
      (!['bishop', 'rook', 'queen'].includes(piece.role) || !anyPieceBetween(piecePos, square, ctx.allPieces))
    );
  });
};

const isFriendlyOnDestAndAttacked = (ctx: cg.MobilityContext): boolean =>
  isDestOccupiedByFriendly(ctx) &&
  (canBeCapturedBySomeEnemyEnPassant(ctx.dest.key, ctx.friendlies, ctx.enemies, ctx.lastMove) ||
    isDestControlledByEnemy(ctx));

const canBeCapturedBySomeEnemyEnPassant = (
  potentialSquareOfFriendlyPawn: cg.Key | undefined,
  friendlies: cg.Pieces,
  enemies: cg.Pieces,
  lastMove?: cg.Key[],
): boolean => {
  if (!potentialSquareOfFriendlyPawn || (lastMove && potentialSquareOfFriendlyPawn !== lastMove[1]))
    return false;
  const pos = util.key2pos(potentialSquareOfFriendlyPawn);
  const friendly = friendlies.get(potentialSquareOfFriendlyPawn);
  return (
    friendly?.role === 'pawn' &&
    pos[1] === (friendly.color === 'white' ? 3 : 4) &&
    (!lastMove || util.diff(util.key2pos(lastMove[0])[1], pos[1]) === 2) &&
    [1, -1].some(delta => {
      const k = util.pos2key([pos[0] + delta, pos[1]]);
      return !!k && enemies.get(k)?.role === 'pawn';
    })
  );
};

const isPathClearEnoughOfFriendliesForPremove = (
  ctx: cg.MobilityContext,
  isPawnAdvance: boolean,
): boolean => {
  if (ctx.unrestrictedPremoves) return true;
  const squaresBetween = util.squaresBetween(...ctx.orig.pos, ...ctx.dest.pos);
  if (isPawnAdvance) squaresBetween.push(ctx.dest.key);
  const squaresOfFriendliesBetween = squaresBetween.filter(s => ctx.friendlies.has(s));
  if (!squaresOfFriendliesBetween.length) return true;
  const firstSquareOfFriendliesBetween = squaresOfFriendliesBetween[0];
  const nextSquare = util.squareShiftedVertically(
    firstSquareOfFriendliesBetween,
    ctx.color === 'white' ? -1 : 1,
  );
  return (
    squaresOfFriendliesBetween.length === 1 &&
    canBeCapturedBySomeEnemyEnPassant(
      firstSquareOfFriendliesBetween,
      ctx.friendlies,
      ctx.enemies,
      ctx.lastMove,
    ) &&
    !!nextSquare &&
    !squaresBetween.includes(nextSquare)
  );
};

const isPathClearEnoughOfEnemiesForPremove = (ctx: cg.MobilityContext, isPawnAdvance: boolean): boolean => {
  if (ctx.unrestrictedPremoves) return true;
  const squaresBetween = util.squaresBetween(...ctx.orig.pos, ...ctx.dest.pos);
  if (isPawnAdvance) squaresBetween.push(ctx.dest.key);
  const squaresOfEnemiesBetween = squaresBetween.filter(s => ctx.enemies.has(s));
  if (squaresOfEnemiesBetween.length > 1) return false;
  if (!squaresOfEnemiesBetween.length) return true;
  const enemySquare = squaresOfEnemiesBetween[0];
  const enemy = ctx.enemies.get(enemySquare);
  if (!enemy || enemy.role !== 'pawn') return true;

  const enemyStep = enemy.color === 'white' ? 1 : -1;
  const squareAbove = util.squareShiftedVertically(enemySquare, enemyStep);
  const enemyPawnDests: cg.Key[] = squareAbove
    ? [
        ...util.adjacentSquares(squareAbove).filter(s => canEnemyPawnCaptureOnSquare(enemySquare, s, ctx)),
        ...[squareAbove, util.squareShiftedVertically(squareAbove, enemyStep)]
          .filter(s => !!s)
          .filter(s => canEnemyPawnAdvanceToSquare(enemySquare, s, ctx)),
      ]
    : [];
  const badSquares = [...squaresBetween, ctx.orig.key];
  return enemyPawnDests.some(square => !badSquares.includes(square));
};

const isPathClearEnoughForPremove = (ctx: cg.MobilityContext, isPawnAdvance: boolean): boolean =>
  isPathClearEnoughOfFriendliesForPremove(ctx, isPawnAdvance) &&
  isPathClearEnoughOfEnemiesForPremove(ctx, isPawnAdvance);

const pawn: cg.Mobility = (ctx: cg.MobilityContext) => {
  const step = ctx.color === 'white' ? 1 : -1;
  if (util.diff(ctx.orig.pos[0], ctx.dest.pos[0]) > 1) return false;
  if (!util.diff(ctx.orig.pos[0], ctx.dest.pos[0]))
    return (
      util.pawnDirAdvance(...ctx.orig.pos, ...ctx.dest.pos, ctx.color === 'white') &&
      isPathClearEnoughForPremove(ctx, true)
    );
  if (ctx.dest.pos[1] !== ctx.orig.pos[1] + step) return false;
  if (ctx.unrestrictedPremoves || isDestOccupiedByEnemy(ctx)) return true;
  if (isDestOccupiedByFriendly(ctx)) return isDestControlledByEnemy(ctx);
  else
    return (
      canSomeEnemyPawnAdvanceToDest(ctx) ||
      canBeCapturedBySomeEnemyEnPassant(
        util.pos2key([ctx.dest.pos[0], ctx.dest.pos[1] + step]),
        ctx.friendlies,
        ctx.enemies,
        ctx.lastMove,
      ) ||
      isDestControlledByEnemy(ctx, ['pawn'])
    );
};

const knight: cg.Mobility = (ctx: cg.MobilityContext) =>
  util.knightDir(...ctx.orig.pos, ...ctx.dest.pos) &&
  (ctx.unrestrictedPremoves || !isDestOccupiedByFriendly(ctx) || isFriendlyOnDestAndAttacked(ctx));

const bishop: cg.Mobility = (ctx: cg.MobilityContext) =>
  util.bishopDir(...ctx.orig.pos, ...ctx.dest.pos) &&
  isPathClearEnoughForPremove(ctx, false) &&
  (ctx.unrestrictedPremoves || !isDestOccupiedByFriendly(ctx) || isFriendlyOnDestAndAttacked(ctx));

const rook: cg.Mobility = (ctx: cg.MobilityContext) =>
  util.rookDir(...ctx.orig.pos, ...ctx.dest.pos) &&
  isPathClearEnoughForPremove(ctx, false) &&
  (ctx.unrestrictedPremoves || !isDestOccupiedByFriendly(ctx) || isFriendlyOnDestAndAttacked(ctx));

const queen: cg.Mobility = (ctx: cg.MobilityContext) => bishop(ctx) || rook(ctx);

const king: cg.Mobility = (ctx: cg.MobilityContext) =>
  (util.kingDirNonCastling(...ctx.orig.pos, ...ctx.dest.pos) &&
    (ctx.unrestrictedPremoves || !isDestOccupiedByFriendly(ctx) || isFriendlyOnDestAndAttacked(ctx))) ||
  (ctx.canCastle &&
    ctx.orig.pos[1] === ctx.dest.pos[1] &&
    ctx.orig.pos[1] === (ctx.color === 'white' ? 0 : 7) &&
    ((ctx.orig.pos[0] === 4 &&
      ((ctx.dest.pos[0] === 2 && ctx.rookFilesFriendlies.includes(0)) ||
        (ctx.dest.pos[0] === 6 && ctx.rookFilesFriendlies.includes(7)))) ||
      ctx.rookFilesFriendlies.includes(ctx.dest.pos[0])) &&
    (ctx.unrestrictedPremoves ||
      /* The following checks if no non-rook friendly piece is in the way between the king and its castling destination.
         Note that for the Chess960 edge case of Kb1 "long castling", the check passes even if there is a piece in the way
         on c1. But this is fine, since premoving from b1 to a1 as a normal move would have already returned true. */
      util
        .squaresBetween(...ctx.orig.pos, ctx.dest.pos[0] > ctx.orig.pos[0] ? 7 : 1, ctx.dest.pos[1])
        .map(s => ctx.allPieces.get(s))
        .every(p => !p || util.samePiece(p, { role: 'rook', color: ctx.color }))));

const mobilityByRole = { pawn, knight, bishop, rook, queen, king };

export const additionalPremoveRequirements: cg.Mobility = (ctx: cg.MobilityContext) =>
  mobilityByRole[ctx.role](ctx);
