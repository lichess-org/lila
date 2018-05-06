$(function() {
  load($('form.login'));
});

function load($f) {
  $f.submit(function() {
    $f.find('.submit').attr('disabled', true);
    $.ajax({
      url: $f.attr('action'),
      method: $f.attr('method'),
      data: {
        username: $f.find('.username input').val(),
        password: $f.find('.password input').val(),
        token: $f.find('.token input').val()
      },
      success: function(res) {
        if (res === '2fa') {
          $f.find('.one-factor').hide();
          $f.find('.two-factor').show();
          $f.find('.token input').val('');
          $f.find('.submit').attr('disabled', false);
        }
        else lichess.redirect(res);
      },
      error: function(err) {
        $f.replaceWith($(err.responseText).find('form.login'));
        load($('form.login'));
      }
    });
    return false;
  });
}
