import { Clock } from 'puz/clock';

export class Countdown {
  played = new Set<number>();

  public constructor(readonly clock: Clock, readonly onStart: () => void, readonly redraw: () => void) {
    for (let i = 10; i >= 0; i--) lichess.sound.loadStandard(`countDown${i}`);
  }

  start = (startsAt: Date, aloud: boolean): void => {
    const countdown = () => {
      const diff = startsAt.getTime() - Date.now();
      if (diff > 0) {
        if (aloud && diff % 1000 > 500) this.playOnce(Math.ceil(diff / 1000));
        setTimeout(countdown, diff % 1000);
      } else {
        if (aloud) this.playOnce(0);
        this.clock.start();
        this.onStart();
      }
      this.redraw();
    };
    countdown();
  };

  private playOnce = (i: number) => {
    if (!this.played.has(i)) {
      this.played.add(i);
      lichess.sound.play(`countDown${i}`);
    }
  };
}
