export default function() {
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
}
