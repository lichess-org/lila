import { runner, fixMainBoardHeight } from 'common/gridHacks';

let booted = false;

export function start(container: HTMLElement) {

  /* Detected browsers with a correct grid min-content implementation:
   * Chrome, Chromium, Brave, Opera, Safari 12+
   */
  if (window.chrome) return;

  const runHacks = () => fixMainBoardHeight(container);

  runner(runHacks);

  if (!booted) {
    booted = true;
    document.body.addEventListener('chessground.resize', runHacks);
  }
}
