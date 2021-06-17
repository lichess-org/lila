function parseFen($elem) {
  $elem.each(function () {
    var $this = $(this).removeClass('parse-fen');
    var lm = $this.data('lastmove');
    var dropOrMove = lm ? (lm.includes('*') ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]) : undefined;
    var fen = $this.data('fen');
    var hands = fen && fen.split(' ').length > 2 ? fen.split(' ')[2] : '';
    var color = $this.data('color');
    var ground = $this.data('shogiground');
    var config = {
      coordinates: false,
      resizable: false,
      drawable: { enabled: false, visible: false },
      viewOnly: true,
      hasPockets: true,
      pockets: hands,
      fen: fen,
      lastMove: dropOrMove,
    };
    if (color) config.orientation = color;
    if (ground) ground.set(config);
    else {
      this.innerHTML = '<div class="cg-wrap mini-board"></div>';
      $this.data('shogiground', Shogiground(this.firstChild, config));
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
    return $featured.find('.mini-board');
  };
  parseFen(board());
  if (!window.EventSource) return;
  var source = new EventSource($('body').data('stream-url'));
  source.addEventListener(
    'message',
    function (e) {
      var data = JSON.parse(e.data);
      if (data.t == 'featured') {
        $featured.html(data.d.html).find('a').attr('target', '_blank');
        parseFen(board());
      } else if (data.t == 'fen') {
        parseFen(board().data('fen', data.d.fen).data('lastmove', data.d.lm));
      }
    },
    false
  );
  resize();
  window.addEventListener('resize', resize);
});
