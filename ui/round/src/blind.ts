import RoundController from './ctrl';

let handler: any;

export function init(el: HTMLElement, ctrl: RoundController) {
  if (window.lichess.NVUI) handler = window.lichess.NVUI(el, ctrl);
  else window.lichess.loadScript('compiled/nvui.min.js').then(() => init(el, ctrl));
}
export function reload() {
  if (handler) handler.reload();
}
