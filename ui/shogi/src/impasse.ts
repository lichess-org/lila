import { parseSfen } from 'shogiops/sfen';
import { Rules } from 'shogiops/types';
import { promotionZone } from 'shogiops/variant/util';

export interface ImpasseInfo {
  king: boolean;
  nbOfPieces: number;
  pieceValue: number;
  check?: boolean;
}
export interface ImpasseInfoByColor {
  sente: ImpasseInfo;
  gote: ImpasseInfo;
}

export function impasseInfo(rules: Rules, sfen: Sfen, initialSfen?: Sfen): ImpasseInfoByColor | undefined {
  if (!['standard', 'annanshogi', 'checkshogi'].includes(rules)) return;

  const shogi = parseSfen(rules, sfen, false),
    pointOffset = initialSfen ? pointOffsetFromSfen(initialSfen) : 0;

  if (shogi.isErr) return;

  const board = shogi.value.board;
  const sentePromotion = promotionZone(rules)('sente').intersect(board.color('sente')),
    gotePromotion = promotionZone(rules)('gote').intersect(board.color('gote')),
    allMajorPieces = board
      .role('bishop')
      .union(board.role('rook'))
      .union(board.role('horse'))
      .union(board.role('dragon'));

  const senteKing: boolean = !sentePromotion.intersect(board.role('king')).isEmpty(),
    goteKing: boolean = !gotePromotion.intersect(board.role('king')).isEmpty();

  const senteNumberOfPieces: number = sentePromotion.diff(board.role('king')).size(),
    goteNumberOfPieces: number = gotePromotion.diff(board.role('king')).size();

  const senteImpasseValue =
    senteNumberOfPieces +
    allMajorPieces.intersect(sentePromotion).size() * 4 +
    shogi.value.hands.color('sente').count() +
    (shogi.value.hands.color('sente').get('bishop') + shogi.value.hands.color('sente').get('rook')) * 4;

  const goteImpasseValue =
    pointOffset +
    goteNumberOfPieces +
    allMajorPieces.intersect(gotePromotion).size() * 4 +
    shogi.value.hands.color('gote').count() +
    (shogi.value.hands.color('gote').get('bishop') + shogi.value.hands.color('gote').get('rook')) * 4;

  return {
    sente: {
      king: senteKing,
      nbOfPieces: senteNumberOfPieces,
      pieceValue: senteImpasseValue,
      check: shogi.value.turn === 'sente' && shogi.value.isCheck(),
    },
    gote: {
      king: goteKing,
      nbOfPieces: goteNumberOfPieces,
      pieceValue: goteImpasseValue,
      check: shogi.value.turn === 'gote' && shogi.value.isCheck(),
    },
  };
}

export function isImpasse(rules: Rules, sfen: Sfen, initialSfen?: Sfen): boolean {
  const info = impasseInfo(rules, sfen, initialSfen);
  if (info) {
    return ['sente', 'gote'].some((color: Color) => {
      const i = info[color];
      return i.king && i.nbOfPieces >= 10 && i.pieceValue >= (color === 'sente' ? 28 : 27) && !i.check;
    });
  } else return false;
}

// https://github.com/WandererXII/scalashogi/blob/main/src/main/scala/StartingPosition.scala
function pointOffsetFromSfen(sfen: string): number {
  switch (sfen.split(' ').slice(0, 3).join(' ')) {
    case 'lnsgkgsn1/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 1;
    case '1nsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 1;
    case 'lnsgkgsnl/1r7/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 5;
    case 'lnsgkgsnl/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 5;
    case 'lnsgkgsn1/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 6;
    case 'lnsgkgsnl/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 10;
    case '1nsgkgsn1/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 12;
    case '2sgkgs2/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 14;
    case '3gkg3/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 16;
    case '4k4/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 18;
    case '4k4/9/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w 3p':
      return 24;
    case '4k4/9/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 27;
    case 'ln2k2nl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 4;
    case 'l3k3l/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -"':
      return 6;
    case '4k4/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -':
      return 8;
    default:
      return 0;
  }
}
