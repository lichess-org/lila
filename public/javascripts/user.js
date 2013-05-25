$(function() {
  if($users = $('div.online_users').orNot()) {
    // Update online users
    var onlineUserUrl = $users.attr('data-reload-url');
    function reloadOnlineUsers() {
      setTimeout(function() {
        $.get(onlineUserUrl, function(html) {
          $users.html(html);
          $('body').trigger('lichess.content_loaded');
          reloadOnlineUsers();
        });
      }, 3000);
    };
    reloadOnlineUsers();
  }

  if($searchForm = $('form.search_user_form').orNot()) {
    $searchInput = $searchForm.find('input.search_user');
    $searchInput.on('autocompleteselect', function(e, ui) {
      setTimeout(function() {$searchForm.submit();},10);
    });
    $searchForm.submit(function() {
      location.href = $searchForm.attr('action')+'/'+$searchInput.val();
      return false;
    });
  }

  $("div.user_show .mod_zone_toggle").click(function() {
    var $zone = $("div.user_show .mod_zone");
    if ($zone.is(':visible')) $zone.hide(); 
    else $zone.html("Loading...").show().load($(this).attr("href"));
    return false;
  });

  $('div.user_bio .editable').on('click', function() {
    $editable = $(this);
    $parent = $(this).parent().addClass('editing');
    $form = $parent.find('form').show();
    $form.find('textarea').val($editable.text());
    function unedit() {
      $form.find('button.cancel').off('click');
      $form.off('submit');
      $parent.removeClass('editing');
    }
    $form.find('button.cancel').on('click', function(e) {
      unedit();
      return false;
    });
    $form.on('submit', function() {
      $.ajax({
        url: $form.attr('action'),
        type: 'PUT',
        data: { bio: $form.find('textarea').val() },
        success: function(t) {
          $editable.text(t);
          unedit();
        }
      });
      return false;
    });
  });
});
function str_repeat(input, multiplier) {
  return new Array(multiplier + 1).join(input);
}
