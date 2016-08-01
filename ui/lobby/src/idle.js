module.exports = function(delay, onIdle, onWakeUp) {
  var events = ['mousemove', 'touchstart'];
  var listening = false;
  var active = true;
  var lastSeenActive = new Date();
  var onActivity = function() {
    if (!active) {
      console.log('Wake up');
      onWakeUp();
    }
    active = true;
    lastSeenActive = new Date();
    stopListening();
  };
  var startListening = function() {
    if (!listening) {
      events.forEach(function(e) {
        document.addEventListener(e, onActivity);
      });
      listening = true;
    }
  };
  var stopListening = function() {
    if (listening) {
      events.forEach(function(e) {
        document.removeEventListener(e, onActivity);
      });
      listening = false;
    }
  };
  setInterval(function() {
    if (active && new Date() - lastSeenActive > delay) {
      console.log('Idle mode');
      onIdle();
      active = false;
    }
    startListening();
  }, 10000);
};
