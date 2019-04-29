
function renderSan(san: San) {
  if (!san) return ''
  const lowerSan = san.toLowerCase(),
    isCapture = lowerSan.toLowerCase().includes('x'),
    fields = lowerSan.split(isCapture ? 'x' : '-');
  if (fields.length <= 1) return san;
  if (isCapture) return [fields[0], 'takes', ...fields.slice(1)].join(' ');
  else return fields.join(' ');
}

export function say(text: string, cut: boolean = false) {
  const msg = new SpeechSynthesisUtterance(text);
  msg.rate = 1.2;
  if (cut) speechSynthesis.cancel();
  window.lidraughts.sound.say(msg);
}

export function step(s: { san?: San}, cut: boolean = true) {
  say(s.san ? renderSan(s.san) : 'Game start', cut);
}
