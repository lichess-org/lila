var $wrap = $('#clinput');
if (!$wrap.length) return;

function toggle() {
  $wrap.toggleClass('shown');
  var $input = $wrap.find('input');
  if (!$wrap.hasClass('init')) {
    $wrap.addClass('init');
    $input
      .on('blur', toggle)
      .on('keypress', function(e) {
        if (e.which == 10 || e.which == 13) {
          execute(e.target.value);
          e.target.value = '';
        }
      });
      lichess.userAutocomplete($input, {
        focus: 1,
        friend: true
      });
  } else $input.focus();
}

function execute(cmd) {
  if (cmd[0] === '/') console.log(cmd);
  else location.href = '/@/' + cmd;
}

Mousetrap.bind('s', function() {
  setTimeout(toggle, 150);
});
$wrap.find('a').on('click', toggle);
