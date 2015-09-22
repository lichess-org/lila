var m = require('mithril');

module.exports = function(opts, name) {

  var instance = new Worker(opts.path);
  var switching = m.prop(false); // when switching to new work, info for previous can be emited

  var send = function(text) {
    instance.postMessage(text);
  };

  var processOutput = function(text, work) {
    if (/currmovenumber|lowerbound|upperbound/.test(text)) return;
    var matches = text.match(/depth (\d+) .*score (cp|mate) ([-\d]+) .*pv (.+)/);
    if (!matches) return;
    var depth = parseInt(matches[1]);
    if (switching() && depth > 1) return; // stale info for previous work
    switching(false); // got depth 1, it's now computing the current work
    if (depth < opts.minDepth) return;
    var cp, mate;
    if (matches[2] === 'cp') cp = parseFloat(matches[3]);
    else mate = parseFloat(matches[3]);
    if (work.ply % 2 === 1) {
      if (matches[2] === 'cp') cp = -cp;
      else mate = -mate;
    }
    var uci = matches[4].split(' ')[0];
    work.emit({
      work: work,
      eval: {
        depth: depth,
        cp: cp,
        mate: mate,
        uci: uci
      },
      name: name
    });
  };

  // warmup
  send('uci');

  return {
    start: function(work) {
      switching(true);
      send(['position', work.position, 'moves', work.moves].join(' '));
      send('go depth ' + opts.maxDepth);
      instance.onmessage = function(msg) {
        processOutput(msg.data, work);
      };
    },
    stop: function() {
      send('stop');
      switching(true);
    }
  };
};
