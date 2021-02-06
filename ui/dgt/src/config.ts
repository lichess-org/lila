export default function () {
  const form = document.getElementById('dgt-config') as HTMLFormElement,
    voiceSelector = document.getElementById('dgt-speech-voice') as HTMLSelectElement;

  (function populateVoiceList() {
    if (typeof speechSynthesis === 'undefined') return;
    speechSynthesis.getVoices().forEach((voice, i) => {
      const option = document.createElement('option');
      option.value = voice.name;
      option.textContent = voice.name + ' (' + voice.lang + ')';
      if (voice.default) option.textContent += ' -- DEFAULT';
      voiceSelector.appendChild(option);
      if (voice.name == localStorage.getItem('dgt-speech-voice')) voiceSelector.selectedIndex = i;
    });
    speechSynthesis.onvoiceschanged = populateVoiceList;
  })();

  const defaultSpeechKeywords = {
    K: 'King',
    Q: 'Queen',
    R: 'Rook',
    B: 'Bishop',
    N: 'Knight',
    P: 'Pawn',
    x: 'Takes',
    '+': 'Check',
    '#': 'Checkmate',
    '(=)': 'Game ends in draw',
    'O-O-O': 'Castles queenside',
    'O-O': 'Castles kingside',
    white: 'White',
    black: 'Black',
    'wins by': 'wins by',
    timeout: 'timeout',
    resignation: 'resignation',
    illegal: 'illegal',
    move: 'move',
  };

  function ensureDefaults() {
    [
      ['dgt-livechess-url', 'ws://localhost:1982/api/v1.0'],
      ['dgt-speech-keywords', JSON.stringify(defaultSpeechKeywords, undefined, 2)],
      ['dgt-speech-synthesis', 'true'],
      ['dgt-speech-announce-all-moves', 'true'],
      ['dgt-speech-announce-move-format', 'san'],
      ['dgt-verbose', 'false'],
    ].forEach(([k, v]) => {
      if (!localStorage.getItem(k)) localStorage.setItem(k, v);
    });
  }

  function populateForm() {
    ['dgt-livechess-url', 'dgt-speech-keywords'].forEach(k => {
      form[k].value = localStorage.getItem(k);
    });
    ['dgt-speech-synthesis', 'dgt-speech-announce-all-moves', 'dgt-verbose'].forEach(k =>
      [true, false].forEach(v => {
        const input = document.getElementById(`${k}_${v}`) as HTMLInputElement;
        input.checked = localStorage.getItem(k) == '' + v;
      })
    );
    ['san', 'uci'].forEach(v => {
      const k = 'dgt-speech-announce-move-format';
      const input = document.getElementById(`${k}_${v}`) as HTMLInputElement;
      input.checked = localStorage.getItem(k) == '' + v;
    });
  }

  ensureDefaults();
  populateForm();

  form.addEventListener('submit', (e: Event) => {
    e.preventDefault();
    Array.from(new FormData(form).entries()).forEach(([k, v]) => localStorage.setItem(k, v.toString()));
  });
}
