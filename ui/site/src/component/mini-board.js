lichess.miniBoard = {
  initAll() {
    Array.from(document.getElementsByClassName('mini-board--init')).forEach(lichess.miniBoard.init);
  },
  init(node) {
    if (!window.Chessground) return setTimeout(() => lichess.miniBoard.init(node), 500);
    const $el = $(node).removeClass('mini-board--init'),
      [fen, orientation, lm] = $el.data('state').split(',');
    $el.data('chessground', Chessground(node, {
      orientation,
      coordinates: false,
      viewOnly: !node.getAttribute('data-playable'),
      resizable: false,
      fen,
      lastMove: lm && (lm[1] === '@' ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]),
      drawable: {
        enabled: false,
        visible: false
      }
    }));
  }
};
