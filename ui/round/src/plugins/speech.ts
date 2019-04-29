const roles: { [letter: string]: string } = { P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king' };

function renderSan(san: San) {
  let move: string;
  if (san.includes('O-O-O')) move = 'long castle';
  else if (san.includes('O-O')) move = 'short castle';
  else move = san.replace(/[\+#]/, '').split('').map(c => {
    if (c == 'x') return 'takes';
    if (c == '+') return 'check';
    if (c == '#') return 'checkmate';
    if (c == '=') return 'promotion';
    if (c == '@') return 'at';
    const code = c.charCodeAt(0);
    if (code > 48 && code < 58) return c; // 1-8
    if (code > 96 && code < 105) return c.toUpperCase();
    return roles[c] || c;
  }).join(' ');
  if (san.includes('+')) move += ' check';
  if (san.includes('#')) move += ' checkmate';
  return move;
}

export function say(text: string, cut: boolean = false) {
  const msg = new SpeechSynthesisUtterance(text);
  msg.rate = 1.2;
  if (cut) speechSynthesis.cancel();
  window.lichess.sound.say(msg);
}

export function step(s: { san?: San}, cut: boolean = true) {
  say(s.san ? renderSan(s.san) : 'Game start', cut);
}
