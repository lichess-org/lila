export function app($wrap: JQuery, toggle: () => void) {
  const $input = $wrap.find('input');
  window.lichess.userAutocomplete($input, {
    focus: 1,
    friend: true,
    onSelect(q: any) {
      execute(q.name || q);
      $input.val('');
    }
  }).done(function() {
    $input.on('blur', () => $('#top').hasClass('clinput') && toggle());
  });
}

function execute(q: string) {
  if (!q) return;
  if (q[0] == '/' || q[1] == '!') command(q.slice(1));
  else location.href = '/@/' + q;
}

function command(q: string) {
  var parts = q.split(' '), exec = parts[0];

  if (exec == 'tv' || exec == 'follow') 
    location.href = '/@/' + parts[1] + '/tv';

  else if (exec == 'play' || exec == 'challenge' || exec == 'match') 
    location.href = '/?user=' + parts[1] + '#friend';

  else if (exec == 'light' || exec == 'dark' || exec == 'transp') 
    getDasher(dasher => dasher.subs.background.set(exec));

  else 
    alert('Unknown command: ' + q);
}

function getDasher(cb: (dasher: any) => void) {
  window.lichess.loadScript(window.lichess.compiledScript('dasher')).done(function() {
    window['LichessDasher'].default(document.createElement('div'), {
      playing: $('body').hasClass('playing')
    }).then(cb);
  });
}
