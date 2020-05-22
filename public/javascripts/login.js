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
        $f.find('.two-factor input').val('').focus();
        $f.find('.submit').attr('disabled', false);
        if (res === 'InvalidTotpToken') $f.find('.two-factor .error').show();
      }
      else location.href = res.startsWith('ok:') ? res.substr(3) : '/';
    };
    cfg.error = function(err) {
      try {
        $f.replaceWith($(err.responseText).find(selector));
        load($(selector));
      } catch(e) {
        alert(err.responseText || 'Error; try again later.');
      }
    };
    $.ajax(cfg);
    return false;
  });
}
