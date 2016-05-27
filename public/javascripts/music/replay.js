function lichessReplayMusic() {

  var orchestra;

  lichess.loadScript('/assets/javascripts/music/orchestra.js').then(function() {
    orchestra = lichessOrchestra();
    $.sound.disable();
  });

  var isPawn = function(san) {
    return san[0] !== san[0].toLowerCase();
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
  var fileToRank = function(file) {
    return 'abcdefgh'.indexOf(file) + 1;
  };

  // c7 = 2 * 8 + 7 = 23
  var keyToInt = function(key) {
    return fileToRank(key[0]) * 8 + parseInt(key[1]);
  };

  var uciToInt = function(uci) {
    return keyToInt(uci.slice(0, 2)) * 64 + keyToInt(uci.slice(2));
  };

  var jump = function(path, node) {
    if (node.san) {
      var pitch = uciToInt(node.uci) / (4096 / 24);
      orchestra.play(isPawn(node.san) ? 'clav' : 'celesta', pitch);
      if (hasCheck(node.san)) orchestra.play('swells', 23 - pitch);
      else if (hasCapture(node.san)) orchestra.play('swells', 23 - pitch);
      else if (hasMate(node.san)) orchestra.play('swells', 23 - pitch);
    } else {
      orchestra.play('swells', 0);
    }
  }

  return {
    jump: function(path, node) {
      if (!orchestra) return;
      jump(path, node);
    }
  };
};
