import debounce from 'debounce-promise';
import { load as loadDasher } from 'dasher';
import { domDialog, alert } from 'lib/view';
import { defined, escapeHtml } from 'lib';
import { complete, type CompleteOpts } from 'lib/view/complete';
import { checkDebouncedResultAgainstTerm, fetchUsers, renderUserEntry } from 'lib/view/userComplete';

type Entry = LightUserOnline | HTMLAnchorElement;

export function initModule({ input }: { input: HTMLInputElement }) {
  const menuLinks = Array.from(document.querySelectorAll<HTMLAnchorElement>('#topnav a')).filter(
    a => a.href != '/',
  );

  const fetchLinks = (term: string): HTMLAnchorElement[] => {
    const all = menuLinks
      .filter(a => a.textContent && a.textContent.toLowerCase().includes(term.toLowerCase()))
      .map(a => a.cloneNode(true) as HTMLAnchorElement)
      .map(a => {
        a.classList.add('complete-result', 'complete-result--menu');
        if (!!a.querySelector('.home')) a.innerHTML = i18n.site.play;
        return a;
      });
    // distinct by href
    const seen = new Set<string>();
    for (let i = all.length - 1; i >= 0; i--) {
      if (seen.has(all[i].href)) all.splice(i, 1);
      else seen.add(all[i].href);
    }
    return all;
  };

  const debouncedXhr = debounce((t: string) => fetchUsers(t, { friend: true }), 150);

  const completeOpts: CompleteOpts<Entry> = {
    input,
    fetch: async t => {
      const users = await debouncedXhr(t).then(checkDebouncedResultAgainstTerm(t));
      return [...fetchLinks(t), ...users];
    },
    render: o => (isLink(o) ? o : renderUserEntry(o)),
    populate: r => r.name,
    onSelect: r => execute(r),
    regex: /^[a-z][\w-]{2,29}$/i,
  };

  complete<Entry>(completeOpts);
  setTimeout(() => input.focus());

  $(input).on('keydown', (e: KeyboardEvent) => {
    if (e.key === 'Enter') {
      execute(input.value);
      input.blur();
    }
  });
}

function execute(e: string | Entry) {
  if (!e) return;
  if (typeof e != 'string' && isLink(e)) location.href = e.href;
  else if (isUser(e)) location.href = '/@/' + e.name;
  else if (e[0] === '/') command(e.replace(/\//g, ''));
  // 5kr1/p1p2p2/2b2Q2/3q2r1/2p4p/2P4P/P2P1PP1/1R1K3R b - - 1 23
  else if (e.match(/^([1-8pnbrqk]+\/){7}.*/i)) location.href = '/analysis/standard/' + e.replace(/ /g, '_');
  else if (e.match(/^[a-zA-Z0-9_-]{2,30}$/)) location.href = '/@/' + e;
  else location.href = '/player/search/' + e;
}

function isLink(e: Entry): e is HTMLAnchorElement {
  return e instanceof HTMLAnchorElement;
}
function isUser(e: string | LightUserOnline): e is LightUserOnline {
  return defined((e as LightUserOnline).name);
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
    modal: true,
    show: true,
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
