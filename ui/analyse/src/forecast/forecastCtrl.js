var m = require('mithril');
var partial = require('chessground').util.partial;

module.exports = function(cfg) {

  var forecasts = [
    [{
      "ply": 4,
      "uci": "h7h5",
      "san": "h5",
      "fen": "rnbqkbnr/pppp1pp1/8/4p2p/3PP3/8/PPP2PPP/RNBQKBNR w KQkq h6 0 3",
      "dests": "iqy cluDMV gvxm dltmvEN eml bqsl fmtAHO pxF jrz nvD ksA owE BJK"
    }, {
      "ply": 5,
      "uci": "h2h3",
      "san": "h3",
      "fen": "rnbqkbnr/pppp1pp1/8/4p2p/3PP3/7P/PPP2PP1/RNBQKBNR b KQkq - 0 3",
      "dests": "NF 90RIzq 5OQ KB !0TV ZRJ 80 2UM YQI ?3V 70TMF 1TL XPH WOG"
    }, {
      "ply": 6,
      "uci": "g7g5",
      "san": "g5",
      "fen": "rnbqkbnr/pppp1p2/8/4p1pp/3PP3/7P/PPP2PP1/RNBQKBNR w KQkq g6 0 4",
      "dests": "iqy cluDM gvm dltmvEN hp eml bqsl fmtAHO jrz nvD xF ksA owE BJK"
    }, {
      "ply": 7,
      "uci": "h3h4",
      "san": "h4",
      "fen": "rnbqkbnr/pppp1p2/8/4p1pp/3PP2P/8/PPP2PP1/RNBQKBNR b KQkq - 0 4",
      "dests": "90RIzq2V 5OQ MEF KB !0TV ZRJ 80 YQI ?3V 70T 1TL XPH WOG"
    }, {
      "ply": 8,
      "uci": "g5h4",
      "san": "gxh4",
      "fen": "rnbqkbnr/pppp1p2/8/4p2p/3PP2p/8/PPP2PP1/RNBQKBNR w KQkq - 0 5",
      "dests": "iqy cluDMV gvxm dltmvEN hpxF eml bqsl fmtAHO jrz nvD ksA owE BJK"
    }],
    [{
      "ply": 4,
      "uci": "b7b5",
      "san": "b5",
      "fen": "rnbqkbnr/p1pp1ppp/8/1p2p3/3PP3/8/PPP2PPP/RNBQKBNR w KQkq b6 0 3",
      "dests": "iqy cluDMV gvxm dltmvEN eml bqsl fmtAH pxF jrz nvD ksA owE BJK"
    }, {
      "ply": 5,
      "uci": "b2b3",
      "san": "b3",
      "fen": "rnbqkbnr/p1pp1ppp/8/1p2p3/3PP3/1P6/P1P2PPP/RNBQKBNR b KQkq - 0 3",
      "dests": "90RIzq 5OQ Hz 6XO KB !0TV 3VN ZRJ 80 2UM YQI 70TMF 1TL WOG"
    }, {
      "ply": 6,
      "uci": "a7a6",
      "san": "a6",
      "fen": "rnbqkbnr/2pp1ppp/p7/1p2p3/3PP3/1P6/P1P2PPP/RNBQKBNR w KQkq - 0 4",
      "dests": "iqy cjqluDMV gvxm dltmvEN eml bqsl fmtAH pxF nvD ksA owE BJK rz"
    }, {
      "ply": 7,
      "uci": "c2c3",
      "san": "c3",
      "fen": "rnbqkbnr/2pp1ppp/p7/1p2p3/3PP3/1PP5/P4PPP/RNBQKBNR b KQkq - 0 4",
      "dests": "90RIzq 5Q OG Hz 6X KB !0TV 3VN ZRJ 80 2UM 4W YQI 70TMF 1TL"
    }, {
      "ply": 8,
      "uci": "f7f5",
      "san": "f5",
      "fen": "rnbqkbnr/2pp2pp/p7/1p2pp2/3PP3/1PP5/P4PPP/RNBQKBNR w KQkq f6 0 5",
      "dests": "iqy cjqluDMV gvxm dltkmvEN eml bql CL sA fmtAH pxF nvD owE BJK rz"
    }, {
      "ply": 9,
      "uci": "d1h5",
      "san": "Qh5+",
      "fen": "rnbqkbnr/2pp2pp/p7/1p2pp1Q/3PP3/1PP5/P4PPP/RNB1KBNR b KQkq - 1 5",
      "dests": "80 2U",
      "check": true
    }],
    [{
      "ply": 4,
      "uci": "b7b5",
      "san": "b5",
      "fen": "rnbqkbnr/p1pp1ppp/8/1p2p3/3PP3/8/PPP2PPP/RNBQKBNR w KQkq b6 0 3",
      "dests": "iqy cluDMV gvxm dltmvEN eml bqsl fmtAH pxF jrz nvD ksA owE BJK"
    }, {
      "ply": 5,
      "uci": "b2b3",
      "san": "b3",
      "fen": "rnbqkbnr/p1pp1ppp/8/1p2p3/3PP3/1P6/P1P2PPP/RNBQKBNR b KQkq - 0 3",
      "dests": "90RIzq 5OQ Hz 6XO KB !0TV 3VN ZRJ 80 2UM YQI 70TMF 1TL WOG"
    }, {
      "ply": 6,
      "uci": "a7a6",
      "san": "a6",
      "fen": "rnbqkbnr/2pp1ppp/p7/1p2p3/3PP3/1P6/P1P2PPP/RNBQKBNR w KQkq - 0 4",
      "dests": "iqy cjqluDMV gvxm dltmvEN eml bqsl fmtAH pxF nvD ksA owE BJK rz"
    }, {
      "ply": 7,
      "uci": "c2c3",
      "san": "c3",
      "fen": "rnbqkbnr/2pp1ppp/p7/1p2p3/3PP3/1PP5/P4PPP/RNBQKBNR b KQkq - 0 4",
      "dests": "90RIzq 5Q OG Hz 6X KB !0TV 3VN ZRJ 80 2UM 4W YQI 70TMF 1TL"
    }, {
      "ply": 8,
      "uci": "f7f5",
      "san": "f5",
      "fen": "rnbqkbnr/2pp2pp/p7/1p2pp2/3PP3/1PP5/P4PPP/RNBQKBNR w KQkq f6 0 5",
      "dests": "iqy cjqluDMV gvxm dltkmvEN eml bql CL sA fmtAH pxF nvD owE BJK rz"
    }, {
      "ply": 9,
      "uci": "d1h5",
      "san": "Qh5+",
      "fen": "rnbqkbnr/2pp2pp/p7/1p2pp1Q/3PP3/1PP5/P4PPP/RNB1KBNR b KQkq - 1 5",
      "dests": "80 2U",
      "check": true
    }, {
      "ply": 10,
      "uci": "g7g6",
      "san": "g6",
      "fen": "rnbqkbnr/2pp3p/p5p1/1p2pp1Q/3PP3/1PP5/P4PPP/RNB1KBNR w KQkq - 0 6",
      "dests": "NV3FxMLUEvmd iqy cjqluDMV gvxm emdl bql CL sA fmtAH pxF nvD owE BJK rz"
    }, {
      "ply": 11,
      "uci": "h5h3",
      "san": "Qh3",
      "fen": "rnbqkbnr/2pp3p/p5p1/1p2pp2/3PP3/1PP4Q/P4PPP/RNB1KBNR b KQkq - 1 6",
      "dests": "90RIzq2V 5Q OG Hz LDC 6X KB !0TV 3VN ZRJ 801 4W YQI 70TMF UM"
    }]
  ];

  var keyOf = function(fc) {
    return fc.map(function(step) {
      return step.ply + ':' + step.uci;
    }).join(',');
  };

  var contains = function(fc1, fc2) {
    return fc1.length >= fc2.length && keyOf(fc1).indexOf(keyOf(fc2)) === 0;
  };

  var collides = function(fc1, fc2) {
    return fc1.length === fc2.length && keyOf(fc1.slice(0, -1)).indexOf(keyOf(fc2.slice(0, -1))) === 0;
  };

  var truncate = function(fc) {
    // must end with player move
    return fc.length % 2 !== 0 ? fc.slice(0, -1) : fc;
  };

  var fixAll = function() {
    // remove contained forecasts
    forecasts = forecasts.filter(function(fc, i) {
      return forecasts.filter(function(f, j) {
        return i !== j && contains(f, fc)
      }).length === 0;
    });
    // remove colliding forecasts
    forecasts = forecasts.filter(function(fc, i) {
      return forecasts.filter(function(f, j) {
        return i < j && collides(f, fc)
      }).length === 0;
    });
  };
  fixAll();

  return {
    addSteps: function(fc) {
      fc = truncate(fc);
      if (fc.length === 0) return;
      forecasts.push(fc);
      fixAll();
    },
    isCandidate: function(fc) {
      fc = truncate(fc);
      if (fc.length < 2) return false;
      var collisions = forecasts.filter(function(f) {
        return contains(f, fc);
      });
      return collisions.length === 0;
    },
    removeIndex: function(index) {
      forecasts = forecasts.filter(function(fc, i) {
        return i !== index;
      });
    },
    list: function() {
      return forecasts;
    },
    gamePly: function() {
      return cfg.gamePly;
    }
  };
};
