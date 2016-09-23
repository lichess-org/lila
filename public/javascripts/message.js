$(function() {
  $root = $('#lichess_message');
  $root.find('select.select').change(function() {
    $root.find('input[name=threads]').prop('checked', false);
    switch ($(this).val()) {
      case 'all':
        $root.find('input[name=threads]').prop('checked', true);
        break;
      case 'read':
        $root.find('tr:not(.new) input[name=threads]').prop('checked', true);
        break;
      case 'unread':
        $root.find('tr.new input[name=threads]').prop('checked', true);
        break;
      case 'study':
        $root.find('tr.new input[name=threads]').prop('checked', true);
        break;
    }
  });
  $root.find('select.action').change(function() {
    var action = $(this).val();
    var ids = [];
    $root.find('input[name=threads]:checked').each(function() {
      return ids.push(this.value);
    });
    if (ids.length === 0) return;
    if (action === 'delete' && !confirm('Delete ' + ids.length + ' message(s)?')) return;
    var url = '/inbox/batch?action=' + action + '&ids=' + ids.join(',');
    var $form = $('<form method="post">').attr('action', url);
    $root.prepend($form);
    $form.submit();
  });
});
