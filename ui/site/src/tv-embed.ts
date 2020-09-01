/* import './component/intro'; */
/* import './component/storage'; */
/* import './component/pubsub'; */
/* import './component/functions'; */
/* import './component/assets'; */
/* import './component/reload'; */
/* import './component/idle-timer'; */
/* import './component/powertip'; */
/* import './component/widget'; */
/* import './component/clock-widget'; */
/* import './component/mini-board'; */
/* import './component/mini-game'; */
/* import './component/modal'; */
/* import './component/is-col1'; */
/* import './component/jquery-ajax'; */
/* import './component/timeago'; */
/* import './component/watchers-widget'; */
/* import './component/friends-widget'; */
/* import './component/trans'; */

function resize() {
  var el = document.querySelector('#featured-game') as HTMLElement;
  if (el.offsetHeight > window.innerHeight)
    el.style.maxWidth = (window.innerHeight - (el.querySelector('.mini-game__player') as HTMLElement).offsetHeight * 2) + 'px';
}

$(function() {
  var $featured = $('#featured-game');
  if (!window.EventSource) return;
  const findGame = () => document.getElementsByClassName('mini-game').item(0) as HTMLElement;
  const setup = () => window.lichess.miniGame.init(findGame());
  setup();
  var source = new EventSource($('body').data('stream-url'));
  source.addEventListener('message', function(e) {
    const msg = JSON.parse(e.data);
    if (msg.t == "featured") {
      $featured.html(msg.d.html).find('a').attr('target', '_blank');
      setup();
    } else if (msg.t == "fen") {
      window.lichess.miniGame.update(findGame(), msg.d);
    }
  }, false);
  resize();
  window.addEventListener('resize', resize);
});
