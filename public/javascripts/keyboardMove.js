function lichessKeyboardMove(opts) {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');
  Mousetrap.bind('enter', function() {
    opts.input.focus();
  });
  opts.input.focus();
  opts.input.addEventListener('keyup', function(e) {
    var v = e.target.value;
    if (v.indexOf('/') > -1) {
      var chatInput = document.querySelector('.mchat input.lichess_say');
      if (chatInput) chatInput.focus();
    } else {
      if (v.length < 2) return;
      if (v.match(/[a-h][1-8]/)) opts.select(v);
    }
    e.target.value = '';
  });
  opts.input.addEventListener('focus', function() {
    opts.focus(true);
  });
  opts.input.addEventListener('blur', function() {
    opts.focus(false);
  });
}
