$(function() {

      console.log("I'm running");

      var reloadTimer = null;

      var startAutoRefresh = function() {
          reloadTimer = setTimeout(function() {
                $("#communication").load("@routes.Mod.publicChats"#
                    public_communication "
    }, 3000);
    };

    var stopAutoRefresh = function () {
    if (reloadTimer) {
    clearTimeout(reloadTimer);
    }
    };
});
