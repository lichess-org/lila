$(function() {
  var $check = $('#tournament #isprivate');
  var $time = $('#tournament tr.time td');
  var $minutes = $('#tournament tr.minutes td');

  function replace($el, $paste) {
      var prvVal = $el.children().first().val();
      $el.html($paste.html());
      if (prvVal) {
        $el.children().first().val(prvVal);
      }
  }

  function showPrivate() {
    var v = $check.prop('checked');
    $('#tournament form').toggleClass('private', v);
    if (v) {
      $('#tournament form input[name=password]').focus();
      replace($time, $('#tournament .private_time'));
      replace($minutes, $('#tournament .private_minutes'));
    } else {
      replace($time, $('#tournament .public_time'));
      replace($minutes, $('#tournament .public_minutes'));
    }
  };
  $check.on('change', showPrivate);
  showPrivate();

  var $variant = $('#tournament tr.variant select');
  var $position = $('#tournament tr.position');
  function showPosition() {
    $position.toggleNone($variant.val() == 1);
  };
  $variant.on('change', showPosition);
  showPosition();

  $('.conditions a.show').on('click', function() {
    $(this).remove();
    $('.conditions .form').show();
  });
});
