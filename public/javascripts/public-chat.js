$(function() {

  var onPageReload = function() {

    var reloadTimer = null;

    var autoRefreshEnabled = true;

    var startAutoRefresh = function() {
      reloadTimer = setTimeout(function() {
        // Reload only the chat grid portions of the page
        $("#lichess").load("/mod/public-chat #communication", function() {
          onPageReload();
        });

      }, 3000);
    };

    var stopAutoRefresh = function() {
      if (reloadTimer) {
        clearTimeout(reloadTimer);
      }
    };

    var addAutoRefreshLink = function() {
      var a = document.createElement('a');
      a.id = "auto_refresh";
      var linkText = document.createTextNode('Auto refresh');
      a.appendChild(linkText);
      a.classList.add('button');
      if (autoRefreshEnabled) a.classList.add('active');

      a.onclick = function() {

        if (autoRefreshEnabled) stopAutoRefresh();
        else startAutoRefresh();

        autoRefreshEnabled = !autoRefreshEnabled;
          $("#auto_refresh").toggleClass('active', autoRefreshEnabled);
      };

      startAutoRefresh();
      $("#communication").append(a);
    };

    addAutoRefreshLink();
  };

  onPageReload();
});
