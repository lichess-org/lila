$(function() {
  var $exists = $('.signup_box form .username .exists');
  var runCheck = lidraughts.fp.debounce(function() {
    var name = $username.val();
    if (name.length >= 3) $.ajax({
      method: 'GET',
      url: '/player/autocomplete',
      data: {
        term: name,
        exists: 1
      },
      success: function(res) {
        $exists.toggle(res);
      }
    });
  }, 300);
  $username = $('.signup_box form input#username')
    .on('change keyup paste', function() {
      $exists.hide();
      runCheck();
    });
});
window.signupSubmit = function(token) {
  document.getElementById("signup_form").submit();
}
