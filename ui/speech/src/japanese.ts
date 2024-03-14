function toHiragana(kanji: string | undefined): string | undefined {
  switch (kanji) {
    case '歩':
      return 'ふ';
    case '香':
      return 'きょう';
    case '桂':
      return 'けい';
    case '銀':
      return 'ぎん';
    case '金':
      return 'きん';
    case '角':
      return 'かく';
    case '飛':
      return 'ひ';
    case 'と':
      return 'と';
    case '成香':
      return 'なりきょう';
    case '成桂':
      return 'なりけい';
    case '成銀':
      return 'なりぎん';
    case '馬':
      return 'うま';
    case '龍':
      return 'りゅう';
    case '玉':
    case '王':
      return 'おう';
    case '成':
      return 'なり';
    case '不成':
      return 'ならず';
    case '打':
      return 'うつ';
    case '直':
      return 'すぐ';
    case '寄':
      return 'よる';
    case '引':
      return 'ひく';
    case '上':
      return 'あがる';
    case '下':
      return 'さがる';
    case '右':
      return 'みぎ';
    case '左':
      return 'ひだり';
    case '同　':
      return 'どう';
    default:
      return kanji;
  }
}
const jReg =
  /((?:[１２３４５６７８９][一二三四五六七八九])|同　)(歩|香|桂|銀|金|角|飛|と|成香|成桂|成銀|馬|龍|玉|王)(打|左|右|上|行|引|寄|直)*(成|不成)?/;

export function renderMoveOrDrop(md: string): string | undefined {
  const match = md.match(jReg);
  if (match)
    return [match[1], match[2], match[3], match[4]]
      .map(toHiragana)
      .filter(s => s && s.length)
      .join(' ');
  else return undefined;
}
