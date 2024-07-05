import { load as loadDasher } from 'dasher';
import { domDialog } from 'common/dialog';
import { escapeHtml } from 'common';

export function initModule({ input }: { input: HTMLInputElement }) {
  site.asset.userComplete({
    input,
    friend: true,
    focus: true,
    onSelect: r => execute(r.name),
  });
  $(input).on('keydown', (e: KeyboardEvent) => {
    if (e.key == 'Enter') {
      execute(input.value);
      input.blur();
    }
  });
}

function execute(q: string) {
  if (!q) return;
  if (q[0] == '/') return command(q.replace(/\//g, ''));
  // 5kr1/p1p2p2/2b2Q2/3q2r1/2p4p/2P4P/P2P1PP1/1R1K3R b - - 1 23
  if (q.match(/^([1-8pnbrqk]+\/){7}.*/i))
    return (location.href = '/analysis/standard/' + q.replace(/ /g, '_'));
  if (q.match(/^[a-zA-Z0-9_-]{2,30}$/)) location.href = '/@/' + q;
  else location.href = '/player/search/' + q;
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
  else if (is('light dark transp system')) loadDasher().then(m => m.background.set(exec));
  else if (is('stream') && parts[1]) location.href = '/streamer/' + parts[1];
  else if (is('help')) help();
  else alert(`Unknown command: "${q}". Type /help for the list of commands`);
}

function commandHelp(aliases: string, args: string, desc: string) {
  return (
    '<div class="command"><div>' +
    aliases
      .split(' ')
      .map(a => `<p>${a} ${escapeHtml(args)}</p>`)
      .join('') +
    `</div> <span>${desc}<span></div>`
  );
}

function help() {
  domDialog({
    css: [{ hashed: 'cli.help' }],
    class: 'clinput-help',
    show: 'modal',
    htmlText:
      '<div><h3>Commands</h3>' +
      commandHelp('/tv /follow', ' <user>', 'Watch someone play') +
      commandHelp('/play /challenge /match', ' <user>', 'Challenge someone to play') +
      commandHelp('/light /dark /transp /system', '', 'Change the background theme') +
      commandHelp('/stream', '<user>', 'Watch someone stream') +
      '<h3>Global hotkeys</h3>' +
      commandHelp('s', '', 'Search for a user') +
      commandHelp('/', '', 'Type a command') +
      commandHelp('c', '', 'Focus the chat input') +
      commandHelp('esc', '', 'Close modals like this one') +
      '</div>',
  });
}
