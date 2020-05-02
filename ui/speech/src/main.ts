
function renderSan(san: San) {
  if (!san) return ''
  const lowerSan = san.toLowerCase(),
    isCapture = lowerSan.toLowerCase().includes('x'),
    fields = lowerSan.split(isCapture ? 'x' : '-');
  if (fields.length <= 1) return san;
  if (isCapture) return [fields[0], 'takes', ...fields.slice(1)].join(' ');
  else return fields.join(' ');
}

export function say(text: string, cut: boolean) {
  const msg = new SpeechSynthesisUtterance(text);
  if (cut) speechSynthesis.cancel();
  window.lidraughts.sound.say(msg);
}

function trimField(f: string) {
  return f.startsWith('0') ? f.slice(1) : f;
}

export function step(s: { san?: San, uci?: Uci }, cut: boolean, captureFrom?: Key) {
  if (captureFrom && s.uci && s.uci.length >= 4) {
    const san = trimField(captureFrom) + 'x' + trimField(s.uci.slice(-2));
    say(renderSan(san), cut);
  } else {
    say(s.san ? renderSan(s.san) : 'Game start', cut);
  }
}
