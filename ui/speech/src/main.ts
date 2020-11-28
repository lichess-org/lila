import {westernShogiNotation} from 'shogiutil/util';

const roles: { [letter: string]: string } = {
  B: "bishop",
  D: "dragon",
  G: "gold",
  H: "horse",
  L: "lance",
  N: "knight",
  P: "pawn",
  R: "rook",
  K: "king",
  S: "silver",
  T: "tokin",
};

function renderSan(san: San) {
  let move: string = westernShogiNotation(san)!;
  if(move[0] === "+") move = "$" + move.substring(1);
  return san
      .split("")
      .map((c) => {
        if (c == "$") return "promoted";
        if (c == "*") return "drop";
        if (c == "x") return "takes";
        if (c == "+") return "promotes";
        if (c == "#") return "checkmate";
        if (c == "=") return "underpromotes";
        const code = c.charCodeAt(0);
        if (code > 48 && code < 59) return c; // 1-9
        if (code > 96 && code < 105) return c.toUpperCase();
        return roles[c] || c;
      })
      .join(" ");
}

function hackFix(msg: string): string {
  return msg
    .replace(/(\d) E (\d)/, "$1,E $2") // Strings such as 1E5 are treated as scientific notation
    .replace(/C /, "c ") // "uppercase C is pronounced as "degrees celsius" when it comes after a number (e.g. R8c3)
    .replace(/F /, "f "); // "uppercase F is pronounced as "degrees fahrenheit" when it comes after a number (e.g. R8f3)
}

export function say(text: string, cut: boolean) {
  const msg = new SpeechSynthesisUtterance(hackFix(text));
  if (cut) speechSynthesis.cancel();
  window.lishogi.sound.say(msg);
}

export function step(s: { san?: San }, cut: boolean) {
  say(s.san ? renderSan(s.san) : "Game start", cut);
}
