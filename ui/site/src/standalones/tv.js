function resize() {
  var el = document.querySelector('#featured-game');
  if (el.offsetHeight > window.innerHeight)
    el.style.maxWidth = (window.innerHeight - el.querySelector('.mini-game__player').offsetHeight * 2) + 'px';
}

$(function() {
  var $featured = $('#featured-game');
  if (!window.EventSource) return;
  const findGame = () => document.getElementsByClassName('mini-game').item(0);
  const setup = () => lichess.miniGame.init(findGame());
  setup();
  var source = new EventSource($('body').data('stream-url'));
  source.addEventListener('message', function(e) {
    const msg = JSON.parse(e.data);
    if (msg.t == "featured") {
      $featured.html(msg.d.html).find('a').attr('target', '_blank');
      setup();
    } else if (msg.t == "fen") {
      lichess.miniGame.update(findGame(), msg.d);
    }
  }, false);
  resize();
  window.addEventListener('resize', resize);
});
