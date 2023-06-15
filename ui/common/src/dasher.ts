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
    const element = $el.empty()[0] as HTMLElement;
    const toggle = $('#top .dasher')[0] as HTMLElement;
    lichess.dasher = lichess.loadModule('dasher').then(() => window.LichessDasher(element, toggle));
  }
  return lichess.dasher;
};
