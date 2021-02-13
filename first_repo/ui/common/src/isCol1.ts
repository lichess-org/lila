let cache: 'init' | 'rec' | boolean = 'init';

export default function (): boolean {
  if (typeof cache == 'string') {
    if (cache == 'init') {
      // only once
      window.addEventListener('resize', () => {
        cache = 'rec';
      }); // recompute on resize
      if (navigator.userAgent.indexOf('Edge/') > -1)
        // edge gets false positive on page load, fix later
        requestAnimationFrame(() => {
          cache = 'rec';
        });
    }
    cache = !!getComputedStyle(document.body).getPropertyValue('--col1');
  }
  return cache;
}
