var m = require('mithril');

module.exports = function(opts, name) {

  var instance = new Worker(opts.path);
  var switching = m.prop(false); // when switching to new work, info for previous can be emited

  var send = function(text) {
    // console.log(name, text);
    instance.postMessage(text);
  };

  var processOutput = function(text, work) {
    if (/currmovenumber|lowerbound|upperbound/.test(text)) return;
    var matches = text.match(/depth (\d+) .*multipv (\d+) .*score (cp|mate) ([-\d]+) .*pv (.+)/);
    if (!matches) return;
    var depth = parseInt(matches[1]);
    if (switching() && depth > 1) return; // stale info for previous work
    switching(false); // got depth 1, it's now computing the current work
    if (depth < opts.minDepth) return;
    var cp, mate, pv = parseInt(matches[2]);
    if (matches[3] === 'cp') cp = parseFloat(matches[4]);
    else mate = parseFloat(matches[4]);
    if (work.ply % 2 === 1) {
      if (matches[3] === 'cp') cp = -cp;
      else mate = -mate;
    }
    var best = matches[5].split(' ')[0];
    console.log(text);
    work.emit({
      work: work,
      eval: {
        depth: depth,
        pv: pv,
        cp: cp,
        mate: mate,
        best: best
      },
      name: name
    });
  };

  // warmup
  send('uci');
  send('setoption name MultiPV value ' + opts.multiPv);

  return {
    start: function(work) {
      switching(true);
      send(['position', 'fen', work.position, 'moves', work.moves].join(' '));
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
