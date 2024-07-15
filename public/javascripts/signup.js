$(function () {
  const $form = $('#signup_form');
  const $exists = $form.find('.username-exists');

  const usernameCheck = lishogi.debounce(function () {
    const name = $username.val();
    if (name.length >= 3)
      $.ajax({
        method: 'GET',
        url: '/api/player/autocomplete',
        data: {
          term: name,
          exists: 1,
        },
        success: function (res) {
          $exists.toggle(res);
        },
      });
  }, 300);

  $username = $form.find('input[name="username"]').on('change keyup paste', function () {
    $exists.hide();
    usernameCheck();
  });

  $form.on('submit', function () {
    $form
      .find('button.submit')
      .attr('disabled', true)
      .removeAttr('data-icon')
      .addClass('frameless')
      .html(lishogi.spinnerHtml);
  });
});
window.signupSubmit = function (token) {
  const form = document.getElementById('signup_form');
  if (form.reportValidity()) form.submit();
};
