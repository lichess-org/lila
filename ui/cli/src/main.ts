import modal from 'common/modal';

const li = window.lichess;

export function app($wrap: JQuery, toggle: () => void) {
  const $input = $wrap.find('input');
  li.userAutocomplete($input, {
    focus: true,
    friend: true,
    onSelect(q: any) {
      $input.val('').blur();
      execute(q.name || q.trim());
      $('body').hasClass('clinput') && toggle()
    }
  }).then(() =>
    $input.on('blur', () => {
      $input.val('');
      $('body').hasClass('clinput') && toggle()
    })
  );
}

function execute(q: string) {
  if (!q) return;
  if (q[0] == '/') return command(q.replace(/\//g, ''));
  else location.href = '/@/' + q;
}

function command(q: string) {
  var parts = q.split(' '), exec = parts[0];

  const is = function(commands: string) {
    return commands.split(' ').includes(exec);
  };

  if (is('tv follow') && parts[1])
    location.href = '/@/' + parts[1] + '/tv';

  else if (is('tv'))
    location.href = '/tv';

  else if (is('play challenge match') && parts[1])
    location.href = '/?user=' + parts[1] + '#friend';

  else if (is('light dark transp'))
    getDasher(dasher => dasher.subs.background.set(exec));

  else if (is('stream') && parts[1])
    location.href = '/streamer/' + parts[1];

  else if (is('help')) help();

  else alert(`Unknown command: "${q}". Type /help for the list of commands`);
}

function commandHelp(aliases: string, args: string, desc: string) {
  return '<div class="command"><div>' +
    aliases.split(' ').map(a => `<p>${a} ${li.escapeHtml(args)}</p>`).join('') +
    `</div> <span>${desc}<span></div>`;
}

function help() {
  li.loadCssPath('clinput.help')
  modal(
    '<h3>Commands</h3>' +
    commandHelp('/tv /follow', ' <user>', 'Watch someone play') +
    commandHelp('/play /challenge /match', ' <user>', 'Challenge someone to play') +
    commandHelp('/light /dark /transp', '', 'Change the background theme') +
    commandHelp('/stream', '<user>', 'Watch someone stream') +
    '<h3>Global hotkeys</h3>' +
    commandHelp('s', '', 'Search for a user') +
    commandHelp('/', '', 'Type a command') +
    commandHelp('c', '', 'Focus the chat input') +
    commandHelp('esc', '', 'Close modals like this one'),
    'clinput-help'
  );
}

function getDasher(cb: (dasher: any) => void) {
  li.loadScript(li.jsModule('dasher')).then(function() {
    window['LichessDasher'](document.createElement('div'), {
      playing: $('body').hasClass('playing')
    }).then(cb);
  });
}
