import { Pieces, Pos } from '@lichess-org/chessground/types';
import { key2pos, pos2key } from '@lichess-org/chessground/util';
import { keyFromAttrs, MoveStyle, renderKey, transPieceStr } from './chess';

const directions = ['top', 'topRight', 'right', 'bottomRight', 'bottom', 'bottomLeft', 'left', 'topLeft'];
type Direction = (typeof directions)[number];

function getKeysOnRay(originKey: Key, direction: Direction, pov: Color): Key[] {
  const originPos = key2pos(originKey);
  const [fileIndex, rankIndex] = originPos;
  const asWhite = pov === 'white';
  const result = [] as Key[];

  for (let d = 1; d < 8; d++) {
    let possibleKey = [-1, -1];
    switch (direction) {
      case 'top':
        possibleKey = asWhite ? [fileIndex, rankIndex + d] : [fileIndex, rankIndex - d];
        break;
      case 'topRight':
        possibleKey = asWhite ? [fileIndex + d, rankIndex + d] : [fileIndex - d, rankIndex - d];
        break;
      case 'right':
        possibleKey = asWhite ? [fileIndex + d, rankIndex] : [fileIndex - d, rankIndex];
        break;
      case 'bottomRight':
        possibleKey = asWhite ? [fileIndex + d, rankIndex - d] : [fileIndex - d, rankIndex + d];
        break;
      case 'bottom':
        possibleKey = asWhite ? [fileIndex, rankIndex - d] : [fileIndex, rankIndex + d];
        break;
      case 'bottomLeft':
        possibleKey = asWhite ? [fileIndex - d, rankIndex - d] : [fileIndex + d, rankIndex + d];
        break;
      case 'left':
        possibleKey = asWhite ? [fileIndex - d, rankIndex] : [fileIndex + d, rankIndex];
        break;
      case 'topLeft':
        possibleKey = asWhite ? [fileIndex - d, rankIndex + d] : [fileIndex + d, rankIndex - d];
        break;
    }
    if (possibleKey[0] > -1 && possibleKey[0] < 8 && possibleKey[1] > -1 && possibleKey[1] < 8)
      result.push(pos2key(possibleKey as Pos));
    else break;
  }
  return result;
}

export function scanDirectionsHandler(pov: Color, pieces: Pieces, style: MoveStyle) {
  return (ev: KeyboardEvent): void => {
    const target = ev.target as HTMLElement;
    const originKey = keyFromAttrs(target) as Key;
    const currentDirection: Direction | null = target.getAttribute('ray') as Direction | null;

    let nextRay: Key[] = [];
    let nextDirectionIndex = 0;

    if (currentDirection == null) {
      nextDirectionIndex = ev.altKey ? 0 : 1;
    } else {
      nextDirectionIndex = directions.indexOf(currentDirection);
      if ((ev.altKey && nextDirectionIndex % 2 === 0) || (!ev.altKey && nextDirectionIndex % 2 === 1))
        nextDirectionIndex = (nextDirectionIndex + (ev.shiftKey ? 6 : 2)) % 8;
      else nextDirectionIndex = (nextDirectionIndex + (ev.shiftKey ? 7 : 1)) % 8;
    }

    for (let i = 0; i < 4; i++) {
      const rayKeys = getKeysOnRay(originKey, directions[nextDirectionIndex], pov);
      if (rayKeys.length == 0) {
        nextDirectionIndex = (nextDirectionIndex + (ev.shiftKey ? 6 : 2)) % 8;
      } else {
        nextRay = rayKeys;
        target.setAttribute('ray', directions[nextDirectionIndex]);
        break;
      }
    }

    const $boardLive = $('.boardstatus');
    const renderedPieces = nextRay.reduce<string[]>(
      (acc, key) =>
        pieces.get(key)
          ? acc.concat(
              `${renderKey(key, style)} ${transPieceStr(pieces.get(key)!.role, pieces.get(key)!.color, i18n)}`,
            )
          : acc,
      [],
    );
    $boardLive.text(
      `${renderKey(originKey, style)}: ${directions[nextDirectionIndex]}: ${renderedPieces.length > 0 ? renderedPieces.join(' , ') : i18n.site.none}`,
    );
  };
}
