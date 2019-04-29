import { Step } from '../interfaces';

const roles: { [letter: string]: string } = { P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king' };

export function renderSan(san: San) {
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
    if (code > 96 && code < 105) return c + ' ';
    return roles[c] || c;
  }).join(' ');
  if (san.includes('+')) move += ' check';
  if (san.includes('#')) move += ' checkmate';
  return move;
}

window.lichess.RoundSpeech = function() {

  const synth = window.speechSynthesis;

  const volumeStorage = window.lichess.storage.make('sound-volume');

  function say(text: string) {
    const msg = new SpeechSynthesisUtterance(text);
    msg.rate = 1.2;
    msg.volume = parseFloat(volumeStorage.get());
    synth.cancel();
    synth.speak(msg);
  }

  return {
    jump(s: Step) {
      say(s.san ? renderSan(s.san) : 'Game starts');
    }
  };
}
