$(function() {
  if($users = $('div.online_users').orNot()) {
    // Update online users
    var onlineUserUrl = $users.attr('data-reload-url');
    function reloadOnlineUsers() {
      setTimeout(function() {
        $.get(onlineUserUrl, function(html) {
          $users.html(html);
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
    else $zone.show().load($(this).attr("href"));
    return false;
  }).click();
});
function str_repeat(input, multiplier) {
  return new Array(multiplier + 1).join(input);
}
