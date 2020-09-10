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

  // Get a Reference to dgt output console zone - put your UI in there
  const root = document.getElementById('dgt-play-zone') as HTMLDivElement;

  // and your code in here.
  root.innerHTML = '<pre id="dgt-play-zone-log"></pre>'
  window.personalToken = token;

  //Capture console output and send it to root element 'dgt-play-zone' and its inner pre dgt-play-zone-log 
  window.lichess.loadScript('/javascripts/dgt/console-interceptor.js');

  //Loading browserfied version of DGT Electronic Board connector for Lichess Board APIs
  window.lichess.loadScript('/javascripts/dgt/lichess-dgt-boards-browser.js');


}

