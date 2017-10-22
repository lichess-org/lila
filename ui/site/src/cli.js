var $wrap = $('#clinput');

function toggle() {
  $wrap.toggleClass('shown');
  if (!$wrap.hasClass('init')) $wrap
    .addClass('init')
    .find('input')
    .on('blur', toggle)
    .on('keypress', function(e) {
      if (e.which == 10 || e.which == 13) {
        execute(e.target.value);
        e.target.value = '';
      }
    });
  $wrap.find('input').focus()
}

function execute(cmd) {
  console.log(cmd);
}

Mousetrap.bind('s', function() {
  setTimeout(toggle, 150);
});
$wrap.find('a').on('click', toggle);
