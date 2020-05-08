function lidraughtsReplayMusic() {

  var orchestra;

  lidraughts.loadScript('javascripts/music/orchestra.js').then(function() {
    orchestra = lidraughtsOrchestra();
  });

  var orchestra;

  lidraughts.loadScript('javascripts/music/orchestra.js').then(function() {
    orchestra = lidraughtsOrchestra();
  });

  var hasCapture = function(san) {
    return san.includes('x');
  };

  var getPiece = function(fen, field) {
    if (!fen) return 'wm';
    for (let fenPart of fen.split(':')) {
      if (fenPart.length <= 1) continue;
      let first = fenPart.slice(0, 1), clr;
      if (first === 'W') clr = 'w';
      else if (first === 'B') clr = 'b';
      else continue;
      const fenPieces = fenPart.slice(1).split(',');
      for (let fenPiece of fenPieces) {
        if (!fenPiece) continue;
        let fieldNumber, role;
        switch (fenPiece.slice(0, 1)) {
          case 'K':
            role = 'k';
            fieldNumber = fenPiece.slice(1);
            break;
          case 'G':
            role = 'g';
            fieldNumber = fenPiece.slice(1);
            break;
          case 'P':
            role = 'p';
            fieldNumber = fenPiece.slice(1);
            break;
          default:
            role = 'm';
            fieldNumber = fenPiece;
            break;
        }
        if (fieldNumber.length === 1) fieldNumber = '0' + fieldNumber;
        if (fieldNumber === field) {
          return clr + role;
        }
      }
    }
    return 'wm';
  }

  var keyToInt = function(key) {
    let k = parseInt(key), x = (k - 1) % 5, y = ((k - 1) + (5 - (k - 1) % 5)) / 5 - 1;
    return y * 5 + x;
  };

  var range = 23.0, uciBase = 50.0 / range;

  var keyToPitch = function(key) {
    return keyToInt(key) / uciBase;
  };

  var jump = function(node) {
    if (node.san) {
      var piece = getPiece(node.fen, node.uci.slice(-2));
      var pitch = keyToPitch(node.uci.slice(-2));
      var instrument = piece[0] === 'w' ? 'clav' : 'celesta';
      orchestra.play(instrument, pitch);
      if (piece[1] !== 'm') {
        orchestra.play('swells', range / 2 + pitch / 2);
      }
      else if (hasCapture(node.san)) {
        orchestra.play('swells', pitch / 2);
        var capturePitch = keyToPitch(node.uci.slice(-4, -2));
        orchestra.play(instrument, capturePitch);
      }
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
