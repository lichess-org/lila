$(function() {
  var $table = $('.players .slist tbody'),
    simulUrl = $table && $table.data('url');
  if (!$table || !simulUrl) return;
  var addRow = function(username) {
    var userlink, deleteBtn;
    if (!username) {
      username = "(empty)";
      userlink = $('<span>');
    } else if (username.startsWith('error: ')) {
      userlink = $('<span>');
    } else {
      userlink = $('<a>');
      if (!$('#submit_player').prop('disabled')) {
        deleteBtn = $('<a data-icon="L" class="button" title="Remove">');
        deleteBtn.on('click', function() {
          $('#player').val('');
          $.ajax({
            method: 'post',
            url: simulUrl + '/disallow/' + username,
            success: function(result) {
              setTimeout(refreshPlayerTable, 100);
           }
          });
        });
      }
    }
    userlink.addClass('user-link ulpt').append(username);
    if (username) userlink.attr('href', '/@/' + username.toLowerCase());
    $table.append(
      $('<tr>')
        .append($('<td>').append(userlink))
        .append($('<td>', {class: 'action'}).append(deleteBtn))
    );
  }
  var refreshPlayerTable = function() {
    $.ajax({
      method: 'get',
      url: simulUrl + '/allowed',
      success: function(data) {
        if (data.ok) {
          $table.html('');
          if (!data.ok.length) addRow();
          else data.ok.forEach(u => addRow(u));
        } else
          addRow('error: ' + data);
      }
    });
  };
  var submitPlayer = function() {
    var username = $('#player').val();
    if (!username) return;
    $('#player').typeahead('val', '');
    $.ajax({
      method: 'post',
      url: simulUrl + '/allow/' + username,
      success: function(result) {
        setTimeout(refreshPlayerTable, 100);
      }
    });
  };
  $('#custom-gameid').on('keypress', function(ev) {
    if (event.keyCode === 13) submitPlayer();
  });
  $('#submit_player').on('click', submitPlayer);
  refreshPlayerTable();
});