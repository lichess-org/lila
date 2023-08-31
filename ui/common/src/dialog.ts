import * as xhr from './xhr';
import * as licon from './licon';

export interface DialogOpts {
  htmlUrl: string;
  cls: string;
  cssPath?: string;
  attrs?: { [key: string]: string };
  container?: HTMLElement | Cash;
}

export async function showDialog(opts: DialogOpts) {
  const [html] = await Promise.all([
    xhr.text(opts.htmlUrl),
    opts.cssPath ? lichess.loadCssPath(opts.cssPath) : Promise.resolve(),
  ]);

  const dialog = document.createElement('dialog');
  dialog.classList.add(opts.cls);
  dialog.appendChild($as<Element>($(html)));

  const close = () => {
    dialog.remove();
    document.removeEventListener('click', clickOutside);
  };
  const clickOutside = (e: UIEvent) => dialog.contains(e.target as Node) && close();

  for (const [attr, val] of Object.entries(opts.attrs ?? {})) {
    dialog.setAttribute(attr, val);
  }

  $(dialog).prepend($(`<button class="button-none close" data-icon="${licon.X}"></span>`));
  $('button.close', dialog).on('click', close);
  document.addEventListener('click', clickOutside);

  if (opts.container) $(opts.container).prepend(dialog);
  else document.body.appendChild(dialog);

  dialog.showModal();
}
