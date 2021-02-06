let numberFormatter: false | Intl.NumberFormat | null = false;

export const numberFormat = (n: number) => {
  if (numberFormatter === false) numberFormatter = window.Intl && Intl.NumberFormat ? new Intl.NumberFormat() : null;
  if (numberFormatter === null) return '' + n;
  return numberFormatter.format(n);
};

export const numberSpread = (el: HTMLElement, nbSteps: number, duration: number, previous: number) => {
  let displayed: string;
  const display = (prev: number, cur: number, it: number) => {
    const val = numberFormat(Math.round((prev * (nbSteps - 1 - it) + cur * (it + 1)) / nbSteps));
    if (val !== displayed) {
      el.textContent = val;
      displayed = val;
    }
  };
  let timeouts: Timeout[] = [];
  return (nb: number, overrideNbSteps?: number) => {
    if (!el || (!nb && nb !== 0)) return;
    if (overrideNbSteps) nbSteps = Math.abs(overrideNbSteps);
    timeouts.forEach(clearTimeout);
    timeouts = [];
    const prev = previous === 0 ? 0 : previous || nb;
    previous = nb;
    const interv = Math.abs(duration / nbSteps);
    for (let i = 0; i < nbSteps; i++)
      timeouts.push(setTimeout(display.bind(null, prev, nb, i), Math.round(i * interv)));
  };
};
