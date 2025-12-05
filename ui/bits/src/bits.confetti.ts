import confetti from 'canvas-confetti';

function randomInRange(min: number, max: number): number {
  return Math.random() * (max - min) + min;
}

interface ConfettiOpts {
  cannons?: boolean;
  fireworks?: boolean;
}

export function initModule(
  opts: ConfettiOpts = {
    cannons: true,
    fireworks: true,
  },
): void {
  const canvas = document.querySelector('canvas#confetti') as HTMLCanvasElement;

  const party = confetti.create(canvas, {
    disableForReducedMotion: true,
    useWorker: true,
    resize: true,
  });

  if (opts.cannons) {
    const durationMs = 18 * 1000;
    const endAt = Date.now() + durationMs;

    const interval = setInterval(function () {
      const timeLeft = endAt - Date.now();
      if (timeLeft <= 0) clearInterval(interval);
      else cannons();
    }, 250);
  }

  if (opts.fireworks) {
    [50, 1200, 2700].forEach(delay => setTimeout(() => fireworks(), delay));
  }

  const cannons = () => {
    const fire = (custom: confetti.Options) =>
      party({
        scalar: 0.9,
        gravity: 0.3,
        particleCount: randomInRange(15, 30),
        spread: randomInRange(30, 80),
        startVelocity: randomInRange(20, 80),
        ticks: randomInRange(180, 230),
        ...custom,
      });

    // left cannon
    for (const _ in [0, 1])
      fire({
        angle: randomInRange(50, 70),
        drift: randomInRange(0, 1),
        origin: { x: -0.3, y: 1 },
      });

    // right cannon
    for (const _ in [0, 1])
      fire({
        angle: randomInRange(110, 130),
        drift: randomInRange(-1, 0),
        origin: { x: 1.3, y: 1 },
      });
  };

  const fireworks = () => {
    const opts: confetti.Options = {
      spread: 360,
      ticks: 80,
      gravity: 0.15,
      decay: 0.85,
      startVelocity: 30,
      colors: ['FFE400', 'FFBD00', 'E89400', 'FFCA6C', 'FDFFB8'],
    };
    const shoot = () => {
      const origin = { x: randomInRange(0.2, 0.8), y: randomInRange(0.1, 0.5) };
      [0, 150].forEach(d =>
        setTimeout(() => {
          party({
            ...opts,
            origin,
            particleCount: 20,
            shapes: ['star'],
            scalar: 0.5,
          });
          party({
            ...opts,
            origin,
            particleCount: 50,
            shapes: ['circle'],
            scalar: 0.35,
          });
        }, d),
      );
    };
    [0, 100, 200, 300].forEach(d => setTimeout(shoot, d));
  };
}
