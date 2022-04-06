import { loadCssPath } from './assets';

export default function chat(data: any) {
  return new Promise(resolve =>
    requestAnimationFrame(() => {
      data.loadCss = loadCssPath;
      resolve(window.LichessChat(document.querySelector('.mchat')!, data));
    })
  );
}
