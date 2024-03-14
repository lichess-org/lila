import { renderMoveOrDrop as jRenderMoveOrDrop } from './japanese';

function toRole(char: string): string | undefined {
  switch (char) {
    case 'と':
      return 'promoted pawn';
    case '馬':
      return 'horse';
    case '龍':
      return 'dragon';
    case 'R':
    case '飛':
      return 'rook';
    case 'B':
    case '角':
      return 'bishop';
    case 'G':
    case '金':
      return 'gold';
    case 'S':
    case '銀':
      return 'silver';
    case 'N':
    case '桂':
      return 'knight';
    case 'L':
    case '香':
      return 'lance';
    case 'P':
    case '歩':
      return 'pawn';
    case 'K':
    case '玉':
    case '王':
      return 'king';
    default:
      return;
  }
}

function toNumber(digit: string): string | undefined {
  if (parseInt(digit)) return digit;
  switch (digit) {
    case '一':
    case '１':
      return '1';
    case '二':
    case '２':
      return '2';
    case '三':
    case '３':
      return '3';
    case '四':
    case '４':
      return '4';
    case '五':
    case '５':
      return '5';
    case '六':
    case '６':
      return '6';
    case '七':
    case '７':
      return '7';
    case '八':
    case '８':
      return '8';
    case '九':
    case '９':
      return '9';
    default:
      return;
  }
}

function toLetter(str: string): string | undefined {
  return ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'].includes(str) ? str.toUpperCase() : '';
}

function pronounce(str: string): string | undefined {
  switch (str) {
    case '-':
    case '(':
    case ')':
      return '';
    case '*':
      return 'drop';
    case 'x':
      return 'takes';
    case '+':
    case '成':
      return 'promotes';
    case '=':
      return 'unpromotes';
    case '!':
      return 'promoted';
    default:
      return;
  }
}

// P-76, G79-78
// P-7f, G7i-7h
// 歩-76, 金(79)-78
// ７六歩, ７八金直
function renderMoveOrDrop(md: string) {
  // avoiding the collision
  if (md[0] === '+' || md[0] === '成') md = '!' + md.substring(1);
  return md
    .replace('不成', '=')
    .split('')
    .map(c => pronounce(c) || toRole(c) || toNumber(c) || toLetter(c))
    .filter(s => s && s.length)
    .join(' ');
}

export function notation(notation: string | undefined, cut: boolean) {
  window.lishogi.sound.say(
    {
      en: notation ? renderMoveOrDrop(notation) : 'Game start',
      jp: notation ? jRenderMoveOrDrop(notation) : '開始',
    },
    cut
  );
}
