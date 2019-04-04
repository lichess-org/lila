$(function() {
  lidraughts.StrongSocket.defaults.events.reload = function() {
    $('.simul-list__content').load($simulList.data('href'), function() {
      lidraughts.pubsub.emit('content_loaded')();
    });
  };
});
