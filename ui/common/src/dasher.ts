declare global {
  interface Lichess {
    dasher?: Promise<DasherCtrl>;
  }
}

interface DasherCtrl {
  subs: {
    background: {
      set(k: string): void;
    };
  };
}

export const loadDasher = (): Promise<DasherCtrl> => {
  if (!lichess.dasher) {
    const $el = $('#dasher_app').html(`<div class="initiating">${lichess.spinnerHtml}</div>`);
    lichess.dasher = lichess.loadModule('dasher').then(() => window.LichessDasher($el.empty()[0]));
  }
  return lichess.dasher;
};
