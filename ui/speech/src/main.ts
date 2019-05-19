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

function hackFix(msg: string): string {

  return msg
    .replace(/^A /, "A, ") // "A takes" & "A 3" are mispronounced
    .replace(/(\d) E (\d)/, "$1,E $2"); // Strings such as 1E5 are treated as scientific notation
}

export function say(text: string, cut: boolean) {
  const msg = new SpeechSynthesisUtterance(hackFix(text));
  if (cut) speechSynthesis.cancel();
  window.lichess.sound.say(msg);
}

export function step(s: { san?: San }, cut: boolean) {
  say(s.san ? renderSan(s.san) : 'Game start', cut);
}
