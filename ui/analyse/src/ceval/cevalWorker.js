var m = require('mithril');

module.exports = function(opts) {

  var instance = new Worker(opts.path);
  var switching = m.prop(false); // when switching to new work, info for previous can be emited
  var processing = m.prop(false);

  var send = function(text) {
    console.log(text);
    instance.postMessage(text);
  };

  return {
    work: function(work) {
      processing(true);
      switching(true);
      send('stop');
      send(['position', work.position, 'moves', work.moves].join(' '));
      send('go depth ' + opts.maxDepth);
      instance.onmessage = function(msg) {
        if (/currmovenumber|lowerbound|upperbound/.test(msg.data)) return;
        var matches = msg.data.match(/depth (\d+) .*score (cp|mate) ([-\d]+) .*pv (.+)/);
        if (!matches) return;
        console.log(msg.data);
        var depth = parseInt(matches[1]);
        if (switching() && depth > 1) return; // stale info for previous work
        switching(false); // got depth 1, it's now computing the current work
        if (depth < opts.minDepth) return;
        var cp = parseFloat(matches[3]);
        if (work.ply % 2 === 1) cp = -cp;
        var uci = matches[4].split(' ')[0];
        work.emit({
          work: work,
          eval: {
            depth: depth,
            cp: cp,
            uci: uci
          }
        });
        if (depth === opts.maxDepth) processing(false);
      };
    },
    stop: function() {
      send('stop');
    },
    processing: processing
  };
};
