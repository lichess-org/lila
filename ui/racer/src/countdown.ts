import { Clock } from 'puz/clock';
import { Redraw } from 'puz/interfaces';

export class Countdown {
  played = new Set<number>();

  public constructor(readonly clock: Clock, readonly resetGround: () => void, readonly redraw: Redraw) {
    for (let i = 10; i >= 0; i--) lichess.sound.loadStandard(`countDown${i}`);
  }

  // returns updated startsAt
  start = (startsIn: number | undefined, isPlayer: boolean): Date | undefined => {
    if (startsIn) {
      const startsAt = new Date(Date.now() + startsIn);
      if (!this.clock.started()) {
        const countdown = () => {
          const diff = startsAt.getTime() - Date.now();
          if (diff > 0) {
            if (isPlayer) this.playOnce(Math.ceil(diff / 1000));
            setTimeout(countdown, (diff % 1000) + 50);
          } else {
            if (isPlayer) this.playOnce(0);
            this.clock.start();
            this.resetGround();
          }
          this.redraw();
        };
        countdown();
      }
      return startsAt;
    }
  };

  private playOnce = (i: number) => {
    if (!this.played.has(i)) {
      this.played.add(i);
      lichess.sound.play(`countDown${i}`);
    }
  };
}
