$(function() {
  load($('form.login'));
});

function load($f) {
  $f.submit(function() {
    $f.find('.submit').attr('disabled', true).attr('data-icon', null).html(lichess.spinnerHtml);
    $.ajax({
      url: $f.attr('action'),
      method: $f.attr('method'),
      data: {
        username: $f.find('.username input').val(),
        password: $f.find('.password input').val()
      },
      success: function(res) {
        return lichess.redirect(res.substr(3));
      },
      error: function(err) {
        $f.replaceWith($(err.responseText).find('form.login'));
        load($('form.login'));
      }
    });
    return false;
  });
}
