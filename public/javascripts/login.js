var selector = '.auth-login form';

$(function() {
  load($(selector));
});

function load($f) {
  $f.submit(function() {
    $f.find('.submit').attr('disabled', true);
    $.ajax({
      ...lidraughts.formAjax($f),
      success(res) {
        if (res === 'MissingTotpToken' || res === 'InvalidTotpToken') {
          $f.find('.one-factor').hide();
          $f.find('.two-factor').show();
          $f.find('.two-factor input').val('').focus();
          $f.find('.submit').attr('disabled', false);
          if (res === 'InvalidTotpToken') $f.find('.two-factor .error').show();
        }
        else location.href = res.indexOf('ok:') === 0 ? res.substr(3) : '/';
      },
      error(err) {
        try {
          $f.replaceWith($(err.responseText).find(selector));
          load($(selector));
        } catch(e) {
          alert(err.responseText || 'Error; try again later.');
        }
      }
    });
    return false;
  });
}
