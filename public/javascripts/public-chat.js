$(function() {

  var autoRefreshEnabled = true;
  var autoRefreshOnHold = false;

  var renderButton = function() {
    $("#auto_refresh")
      .toggleClass('active', autoRefreshEnabled)
      .toggleClass('hold', autoRefreshOnHold);
  };

  var onPageReload = function() {

    $("#communication").append(
      $('<a id="auto_refresh" class="button">Auto refresh</a>').click(function() {
        autoRefreshEnabled = !autoRefreshEnabled;
        renderButton();
      })
    );
    renderButton();

    $('#communication .chat').each(function() {
      this.scrollTop = 99999;
    });

    $('#communication').on('mouseenter', '.chat', function() {
      autoRefreshOnHold = true;
      $("#auto_refresh").addClass('hold');
    }).on('mouseleave', '.chat', function() {
      autoRefreshOnHold = false;
      $("#auto_refresh").removeClass('hold');
    });
  };
  onPageReload();

  setInterval(function() {

    if (!autoRefreshEnabled || document.visibilityState === 'hidden' || autoRefreshOnHold) return;

    // Reload only the chat grid portions of the page
    $("#comm-wrap").load("/mod/public-chat #communication", onPageReload);

  }, 4000);
});
