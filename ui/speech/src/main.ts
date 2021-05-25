const roles: { [letter: string]: string } = { P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king' };

function renderSan(san: San) {
  if (san.includes('O-O-O')) return 'long castle';
  else if (san.includes('O-O')) return 'short castle';
  else
    return hackFix(
      san
        .split('')
        .map(c => {
          if (c == 'x') return 'takes';
          if (c == '+') return 'check';
          if (c == '#') return 'checkmate';
          if (c == '=') return 'promotes to';
          if (c == '@') return 'at';
          const code = c.charCodeAt(0);
          if (code > 48 && code < 58) return c; // 1-8
          if (code > 96 && code < 105) return c.toUpperCase();
          return roles[c] || c;
        })
        .join(' ')
    );
}

function hackFix(msg: string): string {
  return msg
    .replace(/^A /, 'A, ') // "A takes" & "A 3" are mispronounced
    .replace(/(\d) E (\d)/, '$1,E $2') // Strings such as 1E5 are treated as scientific notation
    .replace(/C /, 'c ') // Capital C is pronounced as "degrees celsius" when it comes after a number (e.g. R8c3)
    .replace(/F /, 'f ') // Capital F is pronounced as "degrees fahrenheit" when it comes after a number (e.g. R8f3)
    .replace(/(\d) H (\d)/, '$1H$2'); // "H" is pronounced as "hour" when it comes after a number with a space (e.g. Rook 5 H 3)
}

export function step(s: { san?: San }, cut?: boolean) {
  lichess.sound.say(s.san ? renderSan(s.san) : 'Game start', cut);
}
