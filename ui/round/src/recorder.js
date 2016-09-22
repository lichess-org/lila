var eventName = 'mousemove';

var count = false;

var listener = function(e) {
  console.log('move');
  count++;
}

module.exports = {

  start: function() {
    count = 0;
    document.addEventListener(eventName, listener);
  },
  stop: function() {
    if (count === false) return false;
    document.removeEventListener(eventName, listener);
    return count;
  }
}
