import confetti from 'canvas-confetti';

function randomInRange(min: number, max: number): number {
  return Math.random() * (max - min) + min;
}

export function initModule(): void {
  const canvas = document.querySelector('canvas#confetti') as HTMLCanvasElement;

  const durationMs = 20 * 1000;
  const endAt = Date.now() + durationMs;

  const interval = setInterval(function() {
    const timeLeft = endAt - Date.now();

    if (timeLeft <= 0) {
      return clearInterval(interval);
    }

    const global: confetti.GlobalOptions = {
      useWorker: true,
    };

    const options: confetti.Options = {
      disableForReducedMotion: true,
      gravity: 0.3,
      particleCount: randomInRange(25, 50),
      scalar: 0.3,
      spread: randomInRange(30, 60),
      startVelocity: randomInRange(20, 60),
    };

    // left cannon
    confetti.create(canvas, global)({
      ...options,
      angle: 60,
      drift: randomInRange(0, 1),
      origin: { x: -0.3, y: 1 },
    });

    // right cannon
    confetti.create(canvas, global)({
      ...options,
      angle: 120,
      drift: randomInRange(-1, 0),
      origin: { x: 1.3, y: 1 },
    });
  }, 250);
}
