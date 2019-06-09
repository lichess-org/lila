var selector = '.auth-login form';

$(function() {
  load($(selector));
});

function load($f) {
  $f.submit(function() {
    $f.find('.submit').attr('disabled', true);
    $.ajax({
      url: $f.attr('action'),
      method: $f.attr('method'),
      data: {
        username: $f.find('#form3-username').val(),
        password: $f.find('#form3-password').val(),
        token: $f.find('#form3-token').val()
      },
      success: function(res) {
        if (res === 'MissingTotpToken' || res === 'InvalidTotpToken') {
          $f.find('.one-factor').hide();
          $f.find('.two-factor').show();
          $f.find('.two-factor input').val('').focus();
          $f.find('.submit').attr('disabled', false);
          if (res === 'InvalidTotpToken') $f.find('.two-factor .error').show();
        }
        else location.href = res.startsWith('ok:') ? res.substr(3) : '/';
      },
      error: function(err) {
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
