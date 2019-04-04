$(function() {
  lichess.StrongSocket.defaults.events.reload = function() {
    $('.simul-list__content').load($simulList.data('href'), function() {
      lichess.pubsub.emit('content_loaded')();
    });
  };
});
