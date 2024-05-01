import * as miniGame from './miniGame';
import { embedChessground } from './asset';

function resize() {
  const el = document.querySelector('#featured-game') as HTMLElement;
  if (el.offsetHeight > window.innerHeight)
    el.style.maxWidth =
      window.innerHeight - (el.querySelector('.mini-game__player') as HTMLElement).offsetHeight * 2 + 'px';
}

window.onload = async () => {
  const makeChessground = (await embedChessground()).Chessground;
  const findGame = () => document.getElementsByClassName('mini-game').item(0) as HTMLElement;
  const setup = () => miniGame.init(findGame(), makeChessground);
  setup();
  if (window.EventSource)
    new EventSource(document.body.getAttribute('data-stream-url')!).addEventListener(
      'message',
      e => {
        const msg = JSON.parse(e.data);
        if (msg.t == 'featured') {
          document.getElementById('featured-game')!.innerHTML = msg.d.html;
          setup();
        } else if (msg.t == 'fen') {
          miniGame.update(findGame(), msg.d);
        }
      },
      false,
    );
  resize();
  window.addEventListener('resize', resize);
};
