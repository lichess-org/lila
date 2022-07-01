lishogi.playMusic = function () {
  var orchestra;

  lishogi.loadScript('javascripts/music/orchestra.js').then(function () {
    orchestra = lishogiOrchestra();
  });

  var isPawn = function (notation) {
    return notation && (notation.includes('P') || notation.includes('歩'));
  };

  // support 12x12 board
  var rankToInt = function (file) {
    return 'abcdefghijkl'.indexOf(file);
  };

  // 7f = (7 - 1) * 12 + 5 = 77
  var keyToInt = function (key) {
    return (parseInt(key[0]) - 1) * 12 + rankToInt(key[1]);
  };

  var usiBase = 122;

  var keyToPitch = function (key) {
    return keyToInt(key) / (usiBase / 23);
  };

  var jump = function (node) {
    if (node.usi) {
      var pitch = keyToPitch(node.usi.slice(2));
      var instrument = isPawn(node.notation) ? 'clav' : 'celesta';
      orchestra.play(instrument, pitch);
      if (node.check) orchestra.play('swells', pitch);
      else if (node.capture) {
        orchestra.play('swells', pitch);
        var capturePitch = keyToPitch(node.usi.slice(0, 2));
        orchestra.play(instrument, capturePitch);
      }
    } else {
      orchestra.play('swells', 0);
    }
  };

  return {
    jump: function (node) {
      if (!orchestra) return;
      jump(node);
    },
  };
};
