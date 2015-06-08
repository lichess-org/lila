$(function() {
  var $check = $('#tournament #isprivate');
  var $time = $('#tournament tr.time td');
  var $minutes = $('#tournament tr.minutes td');
  function showPrivate() {
    if ($check.prop('checked')) {
      $time.html($('#tournament .private_time').html());
      $minutes.html($('#tournament .private_minutes').html());
    } else {
      $time.html($('#tournament .public_time').html());
      $minutes.html($('#tournament .public_minutes').html());
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
