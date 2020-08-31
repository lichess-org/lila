import { TournamentData } from './interfaces';
import notify from 'common/notification';

let countDownTimeout: number | undefined;
const li = window.lichess;

export function init() {
  li.sound.load('tournament1st', 'Tournament1st');
  li.sound.load('tournament2nd', 'Tournament2nd');
  li.sound.load('tournament3rd', 'Tournament3rd');
  li.sound.load('tournamentOther', 'TournamentOther');
}

function doCountDown(targetTime: number) {

  let started = false;

  return function curCounter() {
    let secondsToStart = (targetTime - performance.now()) / 1000;

    // always play the 0 sound before completing.
    let bestTick = Math.max(0, Math.round(secondsToStart));
    if (bestTick <= 10) li.sound['countDown' + bestTick]();

    if (bestTick > 0) {
      let nextTick = Math.min(10, bestTick - 1);
      countDownTimeout = setTimeout(curCounter, 1000 *
        Math.min(1.1, Math.max(0.8, (secondsToStart - nextTick))));
    }

    if (!started && bestTick <= 10) {
      started = true;
      notify('The tournament is starting!');
    }
  };
}

export function end(data: TournamentData) {
  if (!data.me) return;
  if (!data.isRecentlyFinished) return;
  if (!li.once('tournament.end.sound.' + data.id)) return;

  let key = 'Other';
  if (data.me.rank < 4) key = '1st';
  else if (data.me.rank < 11) key = '2nd';
  else if (data.me.rank < 21) key = '3rd';

  li.sound.load('tournament' + key, 'Tournament' + key);
  li.sound['tournament' + key]();
}

export function countDown(data: TournamentData) {
  if (!data.me || !data.secondsToStart) {
    if (countDownTimeout) clearTimeout(countDownTimeout);
    countDownTimeout = undefined;
    return;
  }
  if (countDownTimeout) return;
  if (data.secondsToStart > 60 * 60 * 24) return;

  countDownTimeout = setTimeout(
    doCountDown(performance.now() + 1000 * data.secondsToStart - 100),
    900);  // wait 900ms before starting countdown.

  // Preload countdown sounds.
  for (let i = 10; i >= 0; i--) {
    li.sound.load('countDown' + i, 'CountDown' + i);
    li.sound.collection('countDown' + i); // preload
  }
}
