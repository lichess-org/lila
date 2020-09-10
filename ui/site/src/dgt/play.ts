import { parseFen } from 'chessops/fen';

export default function(token: string) {
  // here's the new setting keys
  [
    'dgt-livechess-url',
    'dgt-speech-keywords',
    'dgt-speech-synthesis',
    'dgt-speech-announce-all-moves',
    'dgt-speech-announce-move-format',
    'dgt-verbose'
  ].forEach(k => {
    console.log(k, localStorage.getItem(k));
  });

  // put your UI in there
  const root = document.getElementById('dgt-play-zone') as HTMLDivElement;

  console.log(parseFen('rnbqkbnr/pppp1ppp/8/8/3pP3/2P5/PP3PPP/RNBQKBNR b KQkq - 1 3'));

  // and your code in here.
  root.innerHTML = 'token: ' + token
}
