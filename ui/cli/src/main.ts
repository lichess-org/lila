import modal from 'common/modal';

export function app(input: HTMLInputElement) {
  lichess.userComplete().then(uac => {
    uac({
      input,
      friend: true,
      focus: true,
      onSelect: r => execute(r.name),
    });
    $(input).on('keydown', (e: KeyboardEvent) => {
      if (e.code == 'Enter') {
        execute(input.value);
        input.blur();
      }
    });
  });
}

function execute(q: string) {
  if (!q) return;
  if (q[0] == '/') return command(q.replace(/\//g, ''));
  else location.href = '/@/' + q;
}

function command(q: string) {
  const parts = q.split(' '),
    exec = parts[0];

  const is = function (commands: string) {
    return commands.split(' ').includes(exec);
  };

  if (is('tv follow') && parts[1]) location.href = '/@/' + parts[1] + '/tv';
  else if (is('tv')) location.href = '/tv';
  else if (is('play challenge match') && parts[1]) location.href = '/?user=' + parts[1] + '#friend';
  else if (is('light dark transp'))
    lichess
      .loadModule('dasher')
      .then(() =>
        window.LichessDasher(document.createElement('div'), {
          playing: $('body').hasClass('playing'),
        })
      )
      .then(dasher => dasher.subs.background.set(exec));
  else if (is('stream') && parts[1]) location.href = '/streamer/' + parts[1];
  else if (is('help')) help();
  else alert(`Unknown command: "${q}". Type /help for the list of commands`);
}

function commandHelp(aliases: string, args: string, desc: string) {
  return (
    '<div class="command"><div>' +
    aliases
      .split(' ')
      .map(a => `<p>${a} ${lichess.escapeHtml(args)}</p>`)
      .join('') +
    `</div> <span>${desc}<span></div>`
  );
}

function help() {
  lichess.loadCssPath('clinput.help');
  modal(
    $(
      '<h3>Commands</h3>' +
        commandHelp('/tv /follow', ' <user>', 'Watch someone play') +
        commandHelp('/play /challenge /match', ' <user>', 'Challenge someone to play') +
        commandHelp('/light /dark /transp', '', 'Change the background theme') +
        commandHelp('/stream', '<user>', 'Watch someone stream') +
        '<h3>Global hotkeys</h3>' +
        commandHelp('s', '', 'Search for a user') +
        commandHelp('/', '', 'Type a command') +
        commandHelp('c', '', 'Focus the chat input') +
        commandHelp('esc', '', 'Close modals like this one')
    ),
    'clinput-help'
  );
}
