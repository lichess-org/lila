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
    if ($check.prop('checked')) {
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
    $position.toggle($variant.val() == 1);
  };
  $variant.on('change', showPosition);
  showPosition();
});
