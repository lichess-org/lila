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
  if (Number.parseInt(digit)) return digit;
  switch (digit) {
    case '一':
    case '１':
    case '子':
      return '1';
    case '二':
    case '２':
    case '丑':
      return '2';
    case '三':
    case '３':
    case '寅':
      return '3';
    case '四':
    case '４':
    case '卯':
      return '4';
    case '五':
    case '５':
    case '辰':
      return '5';
    case '六':
    case '６':
    case '巳':
      return '6';
    case '七':
    case '７':
    case '午':
      return '7';
    case '八':
    case '８':
    case '未':
      return '8';
    case '九':
    case '９':
    case '申':
      return '9';
    case '酉':
      return '10';
    case '戌':
      return '11';
    case '亥':
      return '12';
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
export function renderMoveOrDrop(md: string): string {
  // avoiding the collision
  if (md[0] === '+' || md[0] === '成') md = `!${md.substring(1)}`;
  return md
    .replace('不成', '=')
    .split('')
    .map(c => pronounce(c) || toRole(c) || toNumber(c) || toLetter(c))
    .filter(s => s?.length)
    .join(' ');
}
