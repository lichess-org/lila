var startAt;

var start = function() {
  startAt = new Date();
};

var cancel = function() {
  startAt = null;
};

var stop = function() {
  if (!startAt) return 0;
  var time = new Date() - startAt;
  startAt = null;
  return time;
};

module.exports = {
  start: start,
  cancel: cancel,
  stop: stop
};
