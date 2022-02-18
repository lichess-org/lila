import { loadCssPath } from './assets';

const chat = (data: any) =>
  new Promise(resolve =>
    requestAnimationFrame(() => {
      data.loadCss = loadCssPath;
      resolve(window.NewChessChat(document.querySelector('.mchat')!, data));
    })
  );

export default chat;
