// Copied from shogiops
// todo - refactor and rewrite site in ts and get rid of this
function chushogiForsythToRole(str) {
  switch (str.toLowerCase()) {
    case 'l':
      return 'lance';
    case '+l':
      return 'whitehorse';
    case 'f':
      return 'leopard';
    case '+f':
      return 'bishoppromoted';
    case 'c':
      return 'copper';
    case '+c':
      return 'sidemoverpromoted';
    case 's':
      return 'silver';
    case '+s':
      return 'verticalmoverpromoted';
    case 'g':
      return 'gold';
    case '+g':
      return 'rookpromoted';
    case 'k':
      return 'king';
    case 'e':
      return 'elephant';
    case '+e':
      return 'prince';
    case 'a':
      return 'chariot';
    case '+a':
      return 'whale';
    case 'b':
      return 'bishop';
    case '+b':
      return 'horsepromoted';
    case 't':
      return 'tiger';
    case '+t':
      return 'stag';
    case 'o':
      return 'kirin';
    case '+o':
      return 'lionpromoted';
    case 'x':
      return 'phoenix';
    case '+x':
      return 'queenpromoted';
    case 'm':
      return 'sidemover';
    case '+m':
      return 'boar';
    case 'v':
      return 'verticalmover';
    case '+v':
      return 'ox';
    case 'r':
      return 'rook';
    case '+r':
      return 'dragonpromoted';
    case 'h':
      return 'horse';
    case '+h':
      return 'falcon';
    case 'd':
      return 'dragon';
    case '+d':
      return 'eagle';
    case 'n':
      return 'lion';
    case 'q':
      return 'queen';
    case 'p':
      return 'pawn';
    case '+p':
      return 'promotedpawn';
    case 'i':
      return 'gobetween';
    case '+i':
      return 'elephantpromoted';
    default:
      return;
  }
}
function kyotoshogiForsythToRole(str) {
  switch (str.toLowerCase()) {
    case 'k':
      return 'king';
    case 'p':
      return 'pawn';
    case 'r':
      return 'rook';
    case 's':
      return 'silver';
    case 'b':
      return 'bishop';
    case 'g':
      return 'gold';
    case 'n':
      return 'knight';
    case 't':
      return 'tokin';
    case 'l':
      return 'lance';
    default:
      return;
  }
}

function loadChushogiPieceSprite() {
  if (!document.getElementById('chu-piece-sprite')) {
    const cps = document.body.dataset.chuPieceSet || 'Chu_Ryoko_1Kanji';
    $('head').append(
      $('<link id="chu-piece-sprite" rel="stylesheet" type="text/css" />').attr(
        'href',
        `https://lishogi1.org/assets/piece-css/${cps}.css`
      )
    );
  }
}
function loadKyotoshogiPieceSprite() {
  if (!document.getElementById('kyo-piece-sprite')) {
    const cps = document.body.dataset.kyoPieceSet || 'Kyo_Ryoko_1Kanji';
    $('head').append(
      $('<link id="kyo-piece-sprite" rel="stylesheet" type="text/css" />').attr(
        'href',
        `https://lishogi1.org/assets/piece-css/${cps}.css`
      )
    );
  }
}

function parseSfen($elem) {
  $elem.each(function () {
    const $this = $(this).removeClass('parse-sfen'),
      lm = $this.data('lastmove'),
      dropOrMove = lm ? (lm.includes('*') ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]) : undefined,
      sfen = $this.data('sfen'),
      hands = sfen && sfen.split(' ').length > 2 ? sfen.split(' ')[2] : '',
      color = $this.data('color'),
      variant = $this.data('variant'),
      ground = $this.data('shogiground'),
      handRoles =
        variant === 'chushogi'
          ? []
          : variant === 'minishogi'
            ? ['rook', 'bishop', 'gold', 'silver', 'pawn']
            : variant === 'kyotoshogi'
              ? ['tokin', 'gold', 'silver', 'pawn']
              : ['rook', 'bishop', 'gold', 'silver', 'knight', 'lance', 'pawn'];
    config = {
      coordinates: { enabled: false },
      drawable: { enabled: false, visible: false },
      viewOnly: true,
      sfen: { board: sfen, hands: hands },
      hands: { handRoles: handRoles, inlined: variant !== 'chushogi' },
      lastDests: dropOrMove,
      forsyth: {
        fromForsyth:
          variant === 'chushogi'
            ? chushogiForsythToRole
            : variant === 'kytotoshogi'
              ? kyotoshogiForsythToRole
              : undefined,
      },
    };
    if (variant === 'chushogi') loadChushogiPieceSprite();
    else if (variant === 'kyotoshogi') loadKyotoshogiPieceSprite();
    if (color) config.orientation = color;
    if (ground) ground.set(config);
    else {
      this.innerHTML = '<div class="sg-wrap"></div>';
      $this.data('shogiground', Shogiground(config, { board: this.firstChild }));
    }
  });
}

function resize() {
  var el = document.querySelector('#featured-game');
  if (el.offsetHeight > window.innerHeight)
    el.style.maxWidth = window.innerHeight - el.querySelector('.vstext').offsetHeight + 'px';
}

$(function () {
  var $featured = $('#featured-game');
  var board = function () {
    return $featured.find('a.mini-board');
  };
  parseSfen(board());
  if (!window.EventSource) return;
  var source = new EventSource($('body').data('stream-url'));
  source.addEventListener(
    'message',
    function (e) {
      var data = JSON.parse(e.data);
      if (data.t == 'featured') {
        $featured.html(data.d.html).find('a').attr('target', '_blank');
        parseSfen(board());
      } else if (data.t == 'sfen') {
        parseSfen(board().data('sfen', data.d.sfen).data('lastmove', data.d.lm));
      }
    },
    false
  );
  resize();
  window.addEventListener('resize', resize);
});
