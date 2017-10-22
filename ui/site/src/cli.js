var $wrap = $('#clinput');
if (!$wrap.length) return;

function toggle() {
  $wrap.toggleClass('shown');
  var $input = $wrap.find('input');
  if (!$wrap.hasClass('init')) {
    $wrap.addClass('init');
    lichess.userAutocomplete($input, {
      focus: 1,
      friend: true,
      onSelect: function(q) {
        execute(q);
        $input.val('');
      }
    }).done(function() {
      $input.on('blur', toggle);
    });
  }
  if ($wrap.hasClass('shown')) $input.focus();
}

function execute(q) {
  if (!q) return;
  if (q[0] === '/') command(q.slice(1));
  else location.href = '/@/' + q;
}

function command(q) {
  var parts = q.split(' '), exec = parts[0];
  if (exec === 'tv' || exec === 'follow') location.href = '/@/' + parts[1] + '/tv';
  else if (exec === 'play' || exec === 'challenge' || exec === 'match') location.href = '/?user=' + parts[1] + '#friend';
  else alert('Unknown command: ' + q);
}

Mousetrap.bind('s', function() {
  setTimeout(toggle, 150);
});
$wrap.find('a').on('click', toggle);
