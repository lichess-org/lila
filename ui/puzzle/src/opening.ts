import { initWith as initMiniBoard } from 'common/miniBoard';

site.load.then(() => {
  const rootEl = document.querySelector('.puzzle-openings') as HTMLElement | undefined;
  if (rootEl && !('ontouchstart' in window)) loadBoardTips(rootEl);
});

function loadBoardTips(rootEl: HTMLElement) {
  rootEl.addEventListener('mouseover', e => {
    const el = e.target as HTMLElement;
    if (el.classList.contains('blpt')) makeBoardTip(el, e);
    else {
      const parent = el.parentNode as HTMLElement | undefined;
      if (parent && parent.classList.contains('blpt')) makeBoardTip(parent, e);
    }
  });
}

const makeBoardTip = (el: HTMLElement, e: Event) => {
  $(el)
    .removeClass('blpt')
    .powerTip({
      popupId: 'miniBoard',
      preRender(el) {
        const tipEl = document.getElementById('miniBoard') as HTMLDivElement;
        tipEl.innerHTML = `<div class="mini-board mini-board--init cg-wrap standard is2d"/>`;
        initMiniBoard(tipEl.querySelector('.cg-wrap')!, el.dataset['fen']!, 'white');
      },
    });
  $.powerTip.show(el, e);
};
