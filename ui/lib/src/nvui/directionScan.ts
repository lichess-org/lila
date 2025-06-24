import { Pieces, Pos } from '@lichess-org/chessground/types';
import { key2pos, pos2key } from '@lichess-org/chessground/util';
import { keyFromAttrs, MoveStyle, renderKey, transPieceStr } from './chess';

type Ray = 'top' | 'topRight' | 'right' | 'bottomRight' | 'bottom' | 'bottomLeft' | 'left' | 'topLeft';

function getKeysOnRay(originKey: Key, ray: Ray, pov: Color): Key[] {
  const originPos = key2pos(originKey);
  const fileIndex = originPos[0];
  const rankIndex = originPos[1];

  const result = [] as Key[];

  for (let d = 1; d < 8; d++) {
    let possibleKey = [-1, -1];
    switch (ray) {
      case 'top':
        possibleKey = pov === 'white' ? [fileIndex, rankIndex + d] : [fileIndex, rankIndex - d];
        break;
      case 'topRight':
        possibleKey = pov === 'white' ? [fileIndex + d, rankIndex + d] : [fileIndex - d, rankIndex - d];
        break;
      case 'right':
        possibleKey = pov === 'white' ? [fileIndex + d, rankIndex] : [fileIndex - d, rankIndex];
        break;
      case 'bottomRight':
        possibleKey = pov === 'white' ? [fileIndex + d, rankIndex - d] : [fileIndex - d, rankIndex + d];
        break;
      case 'bottom':
        possibleKey = pov === 'white' ? [fileIndex, rankIndex - d] : [fileIndex, rankIndex + d];
        break;
      case 'bottomLeft':
        possibleKey = pov === 'white' ? [fileIndex - d, rankIndex - d] : [fileIndex + d, rankIndex + d];
        break;
      case 'left':
        possibleKey = pov === 'white' ? [fileIndex - d, rankIndex] : [fileIndex + d, rankIndex];
        break;
      case 'topLeft':
        possibleKey = pov === 'white' ? [fileIndex - d, rankIndex + d] : [fileIndex + d, rankIndex - d];
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
    const currentRay: Ray | null = target.getAttribute('ray') as Ray;
    const directions: Ray[] = [
      'top',
      'topRight',
      'right',
      'bottomRight',
      'bottom',
      'bottomLeft',
      'left',
      'topLeft',
    ];

    let nextRay: Key[] = [];
    let nextDirectionIndex = 0;

    if (currentRay == null) {
      nextDirectionIndex = ev.altKey ? 0 : 1;
    } else {
      nextDirectionIndex = directions.indexOf(currentRay);
      if ((ev.altKey && nextDirectionIndex % 2 === 0) || (!ev.altKey && nextDirectionIndex % 2 === 1))
        nextDirectionIndex = ev.shiftKey ? (nextDirectionIndex + 6) % 8 : (nextDirectionIndex + 2) % 8;
      else nextDirectionIndex = ev.shiftKey ? (nextDirectionIndex + 7) % 8 : (nextDirectionIndex + 1) % 8;
    }

    for (let i = 0; i < 4; i++) {
      const rayKeys = getKeysOnRay(originKey, directions[nextDirectionIndex], pov);
      if (rayKeys.length == 0) {
        nextDirectionIndex = ev.shiftKey ? (nextDirectionIndex + 6) % 8 : (nextDirectionIndex + 2) % 8;
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
