import RoundController from './ctrl';

let handler: any;

export function init(el: HTMLElement, ctrl: RoundController) {
  if (window.lidraughts.NVUI) handler = window.lidraughts.NVUI(el, ctrl);
  else window.lidraughts.loadScript('compiled/nvui.min.js').then(() => init(el, ctrl));
}
export function reload() {
  if (handler) handler.reload();
}
