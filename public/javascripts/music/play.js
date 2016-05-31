function lichessPlayMusic() {

  var orchestra;

  lichess.loadScript('/assets/javascripts/music/orchestra.js').then(function() {
    orchestra = lichessOrchestra();
  });

  var isPawn = function(san) {
    return san[0] === san[0].toLowerCase();
  };
  var isKing = function(san) {
    return san[0] === 'K';
  };

  var hasCastle = function(san) {
    return san.indexOf('O-O') === 0;
  };
  var hasCheck = function(san) {
    return san.indexOf('+') !== -1;
  };
  var hasMate = function(san) {
    return san.indexOf('#') !== -1;
  };
  var hasCapture = function(san) {
    return san.indexOf('x') !== -1;
  };

  // a -> 0
  // c -> 2
  var fileToInt = function(file) {
    return 'abcdefgh'.indexOf(file);
  };

  // c7 = 2 * 8 + 7 = 23
  var keyToInt = function(key) {
    return fileToInt(key[0]) * 8 + parseInt(key[1]) - 1;
  };

  var uciBase = 64;

  var keyToPitch = function(key) {
    return keyToInt(key) / (uciBase / 23)
  };

  var jump = function(node) {
    if (node.san) {
      var pitch = keyToPitch(node.uci.slice(2));
      var instrument = (isPawn(node.san) || isKing(node.san)) ? 'clav' : 'celesta';
      orchestra.play(instrument, pitch);
      if (hasCastle(node.san)) orchestra.play('swells', pitch);
      else if (hasCheck(node.san)) orchestra.play('swells', pitch);
      else if (hasCapture(node.san)) {
        orchestra.play('swells', pitch);
        var capturePitch = keyToPitch(node.uci.slice(0, 2));
        orchestra.play(instrument, capturePitch);
      }
      else if (hasMate(node.san)) orchestra.play('swells', pitch);
    } else {
      orchestra.play('swells', 0);
    }
  };

  return {
    jump: function(node) {
      if (!orchestra) return;
      jump(node);
    }
  };
};
