{
  let isCol1Cache = 'init'; // 'init' | 'rec' | boolean
  lichess.isCol1 = () => {
    if (typeof isCol1Cache == 'string') {
      if (isCol1Cache == 'init') { // only once
        window.addEventListener('resize', () => {
          isCol1Cache = 'rec'
        }); // recompute on resize
        if (navigator.userAgent.indexOf('Edge/') > -1) // edge gets false positive on page load, fix later
          requestAnimationFrame(() => {
            isCol1Cache = 'rec'
          });
      }
      isCol1Cache = !!getComputedStyle(document.body).getPropertyValue('--col1');
    }
    return isCol1Cache;
  };
}
