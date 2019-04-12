$(function() {
  var $table = $('.players .slist tbody');
  if (!$table) return;
  var addRow = function(username) {
    var userlink, deleteBtn;
    if (!username) {
      username = "(empty)";
      userlink = $('<span>');
    } else if (username.startsWith('error: ')) {
      userlink = $('<span>');
    } else {
      userlink = $('<a>');
      deleteBtn = $('<a data-icon="L" class="button" title="Remove">');
      deleteBtn.on('click', function() {
        $('#player').val('');
        $.ajax({
          method: 'post',
          url: location.href + '/remove/' + username,
          success: function(result) {
            if (result == "ok")
              setTimeout(400, refreshPlayerTable());
            else
              alert(result);
         }
        });
      });
    }
    userlink.addClass('user_link').append(username);
    $table.append(
      $('<tr>')
        .append($('<td>').append(userlink))
        .append($('<td>', {class: 'action'}).append(deleteBtn))
    );
  }
  var refreshPlayerTable = function() {
    $.ajax({
      method: 'get',
      url: location.href + '/allowed',
      success: (data) => {
        if (data.ok) {
          $table.html('');
          if (data.ok.length == 0)
            addRow();
          else
            data.ok.forEach(u => addRow(u));
        } else
          addRow('error: ' + data);
      }
    });
  };
  $('#submit_player').on('click', function() {
    var username = $('#player').val();
    if (!username) return;
    $('#player').val('');
    $.ajax({
      method: 'post',
      url: location.href + '/add/' + username,
      success: function(result) {
        if (result == "ok")
          setTimeout(400, refreshPlayerTable());
        else
          alert(result);
      }
    });
  });
  refreshPlayerTable();
});