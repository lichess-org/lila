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
      var enableAutoRefreshText = "Enable auto refresh";
      var disableAutoRefreshText = "Disable auto refresh";

      var a = document.createElement('a');
      a.id = "auto_refresh";
      var linkText = document.createTextNode(disableAutoRefreshText);
      a.appendChild(linkText);
      a.classList.add('button');

      a.onclick = function() {

        if (autoRefreshEnabled) {
          stopAutoRefresh();
          $("#auto_refresh").text(enableAutoRefreshText);
        } else {
          startAutoRefresh();
          $("#auto_refresh").text(disableAutoRefreshText);
        }

        autoRefreshEnabled = !autoRefreshEnabled;
      };

      startAutoRefresh();
      $("#communication").append(a);
    };

    addAutoRefreshLink();
  };

  onPageReload();
});
