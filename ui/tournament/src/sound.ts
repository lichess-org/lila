import notify from 'common/notification';
import { TournamentData } from './interfaces';

let countDownTimeout: number | undefined;

function doCountDown(targetTime: number) {
  let started = false;

  return function curCounter() {
    const secondsToStart = (targetTime - performance.now()) / 1000;

    // always play the 0 sound before completing.
    const bestTick = Math.max(0, Math.round(secondsToStart));
    if (bestTick <= 10) lichess.sound.play('countDown' + bestTick);

    if (bestTick > 0) {
      const nextTick = Math.min(10, bestTick - 1);
      countDownTimeout = setTimeout(curCounter, 1000 * Math.min(1.1, Math.max(0.8, secondsToStart - nextTick)));
    }

    if (!started && bestTick <= 10) {
      started = true;
      notify('The tournament is starting!');
    }
  };
}

export function end(data: TournamentData) {
  if (data.me && data.isRecentlyFinished && lichess.once('tournament.end.sound.' + data.id)) {
    let key = 'Other';
    if (data.me.rank < 4) key = '1st';
    else if (data.me.rank < 11) key = '2nd';
    else if (data.me.rank < 21) key = '3rd';

    lichess.sound.play('tournament' + key);
  }
}

export function countDown(data: TournamentData) {
  if (!data.me || !data.secondsToStart) {
    if (countDownTimeout) clearTimeout(countDownTimeout);
    countDownTimeout = undefined;
    return;
  }
  if (countDownTimeout) return;
  if (data.secondsToStart > 60 * 60 * 24) return;

  countDownTimeout = setTimeout(doCountDown(performance.now() + 1000 * data.secondsToStart - 100), 900); // wait 900ms before starting countdown.

  // Preload countdown sounds.
  for (let i = 10; i >= 0; i--) {
    const s = 'countDown' + i;
    lichess.sound.loadStandard(s);
  }
}
