import { memoize } from './common';

interface DasherCtrl {
  subs: {
    background: {
      set(k: string): void;
    };
  };
}

export const loadDasher = memoize<Promise<DasherCtrl>>(async () => {
  const $el = $('#dasher_app').html(`<div class="initiating">${lichess.spinnerHtml}</div>`);
  await lichess.loadModule('dasher');
  return window.LichessDasher($el.empty()[0]);
});
