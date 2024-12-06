const numberFormat = window.Intl && Intl.NumberFormat ? new Intl.NumberFormat() : null;
const formatNumber = (n: number) => (numberFormat ? numberFormat.format(n) : '' + n);

export const animateNumber = (counter: HTMLElement, duration: number = 2000): void => {
  const value = parseInt(counter.dataset['value'] || '0');
  const startAt = performance.now();
  const stopAt = startAt + duration;
  const animate = () => {
    const now = performance.now();
    if (now >= stopAt) counter.innerText = formatNumber(value);
    else {
      const elapsed = now - startAt;
      const ratio = elapsed / duration;
      const current = Math.ceil(value * ratio);
      counter.innerText = formatNumber(Math.ceil(current));
      requestAnimationFrame(animate);
    }
  };
  animate();
};
