const fenColor = (fen: string) => fen.indexOf(' b') > 0 ? 'black' : 'white';

const init = (node: HTMLElement) => {
  if (!window.Chessground) setTimeout(() => init(node), 200);
  else {
    const [fen, orientation, lm] = node.getAttribute('data-state')!.split(','),
      config = {
        coordinates: false,
        viewOnly: true,
        resizable: false,
        fen,
        orientation,
        lastMove: lm && (lm[1] === '@' ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]),
        drawable: {
          enabled: false,
          visible: false
        }
      },
      $el = $(node).removeClass('mini-game--init'),
      $cg = $el.find('.cg-wrap'),
      turnColor = fenColor(fen);
    $cg.data('chessground', window.Chessground($cg[0], config));
    ['white', 'black'].forEach(color =>
      $el.find('.mini-game__clock--' + color).each(function(this: HTMLElement) {
        $(this).clock({
          time: parseInt(this.getAttribute('data-time')!),
          pause: color != turnColor
        });
      })
    );
  }
  return node.getAttribute('data-live');
}

interface UpdateData {
  lm: string;
  fen: string;
  wc?: number;
  bc?: number;
}

const miniGame = {
  init,
  initAll() {
    const nodes = Array.from(document.getElementsByClassName('mini-game--init')),
      ids = nodes.map(init).filter(id => id);
    if (ids.length) window.lichess.StrongSocket.firstConnect.then(send =>
      send('startWatching', ids.join(' '))
    );
  },
  update(node: HTMLElement, data: UpdateData) {
    const $el = $(node),
      lm = data.lm,
      lastMove = lm && (lm[1] === '@' ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]),
      cg = $el.find('.cg-wrap').data('chessground');
    if (cg) cg.set({
      fen: data.fen,
      lastMove
    });
    const turnColor = fenColor(data.fen);
    const renderClock = (time: number | undefined, color: string) => {
      if (!isNaN(time!)) $el.find('.mini-game__clock--' + color).clock('set', {
        time,
        pause: color != turnColor
      });
    };
    renderClock(data.wc, 'white');
    renderClock(data.bc, 'black');
  },
  finish(node: HTMLElement, win?: string) {
    ['white', 'black'].forEach(color => {
      const $clock = $(node).find('.mini-game__clock--' + color).each(function(this: HTMLElement) {
        $(this).clock('destroy');
      });
      if (!$clock.data('managed')) // snabbdom
        $clock.replaceWith(`<span class="mini-game__result">${win ? (win == color[0] ? 1 : 0) : '½'}</span>`)
    });
  }
}

export default miniGame;
