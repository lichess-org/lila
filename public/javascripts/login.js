var selector = '.auth-login form';

$(function() {
  load($(selector));
});

function load($f) {
  $f.submit(function() {
    $f.find('.submit').attr('disabled', true);
    const cfg = lichess.formAjax($f);
    cfg.success = function(res) {
      if (res === 'MissingTotpToken' || res === 'InvalidTotpToken') {
        $f.find('.one-factor').hide();
        $f.find('.two-factor').show();
        requestAnimationFrame(function() {
          $f.find('.two-factor input').val('').focus();
        });
        $f.find('.submit').attr('disabled', false);
        if (res === 'InvalidTotpToken') $f.find('.two-factor .error').show();
      }
      else location.href = res.startsWith('ok:') ? res.substr(3) : '/';
    };
    cfg.error = function(err) {
      const el = $(err.responseText).find(selector);
      if (el.length) {
        $f.replaceWith(el);
        load($(selector));
      } else {
        alert(err.responseText || (err.statusText + '. Please wait some time before trying again.'));
        $f.find('.submit').attr('disabled', false);
      }
    };
    $.ajax(cfg);
    return false;
  });
}
