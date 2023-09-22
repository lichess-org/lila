import * as chessops from 'chessops';

declare global {
  module co {
    const san: typeof chessops.san;
    const fen: typeof chessops.fen;
    const pgn: typeof chessops.pgn;

    const RULES: typeof chessops.RULES;
    const isDrop: typeof chessops.isDrop;
    const isNormal: typeof chessops.isNormal;
    const charToRole: typeof chessops.charToRole;
    const defined: typeof chessops.defined;
    const kingCastlesTo: typeof chessops.kingCastlesTo;
    const makeSquare: typeof chessops.makeSquare;
    const makeUci: typeof chessops.makeUci;
    const opposite: typeof chessops.opposite;
    const parseSquare: typeof chessops.parseSquare;
    const parseUci: typeof chessops.parseUci;
    const roleToChar: typeof chessops.roleToChar;
    const squareFile: typeof chessops.squareFile;
    const squareRank: typeof chessops.squareRank;
    const defaultSetup: typeof chessops.defaultSetup;
    const compat: {
      chessgroundDests: typeof chessops.compat.chessgroundDests;
      scalachessCharPair: typeof chessops.compat.scalachessCharPair;
      lichessRules: typeof chessops.compat.lichessRules;
      lichessVariant: typeof chessops.compat.lichessVariant;
    };
    const variant: {
      castlingSide: typeof chessops.variant.castlingSide;
      defaultPosition: typeof chessops.variant.defaultPosition;
      isStandardMaterial: typeof chessops.variant.isStandardMaterial;
      normalizeMove: typeof chessops.variant.normalizeMove;
      setupPosition: typeof chessops.variant.setupPosition;
    };
    const debug: { board: typeof chessops.debug.board };

    const Chess: Static<chessops.Chess, Unwrappable<chessops.Chess>>;
    const Castles: Static<chessops.Castles>;
    const Board: Static<chessops.Board>;
    const Material: Static<chessops.Material>;
    const RemainingChecks: Static<chessops.RemainingChecks>;

    type Setup = chessops.Setup;
    type Chess = chessops.Chess;
    type Castles = chessops.Castles;
    type Board = chessops.Board;
    type Material = chessops.Material;
    type RemainingChecks = chessops.RemainingChecks;
    type Square = chessops.Square;
    type SquareSet = chessops.SquareSet;
    type SquareName = chessops.SquareName;
    type Color = chessops.Color;
    type Piece = chessops.Piece;
    type Role = chessops.Role;
    type Position = chessops.Position;
    type PositionError = chessops.PositionError;
    type Rules = chessops.Rules;
    type Move = chessops.Move;
    type NormalMove = chessops.NormalMove;
    type FileName = chessops.FileName;
    type RankName = chessops.RankName;
    type Outcome = chessops.Outcome;
    type ChildNode<T> = chessops.pgn.ChildNode<T>;
    type PgnNodeData = chessops.pgn.PgnNodeData;
  }
}

interface Static<T, U = T> {
  default: () => T;
  empty: () => T;
  fromSetup: (setup: chessops.Setup) => U;
}

type Unwrappable<U> = { unwrap: () => U }; // for @badrap/result
