export function resize(el: HTMLElement): void {
  const vstextHeight = el.querySelector<HTMLElement>('.vstext')?.offsetHeight || 0;
  if (el.offsetHeight > window.innerHeight)
    el.style.maxWidth = `${window.innerHeight - vstextHeight}px`;
}
