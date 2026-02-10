// no side effects allowed due to re-export by index.ts

import { type Dialog, domDialog } from './dialog';
import { escapeHtml } from '../index';

// non-blocking window.alert-alike
export async function alert(msg: string): Promise<void> {
  await domDialog({
    htmlText: $html`<div>${escapeHtmlAddBreaks(msg)}</div>
    <span><button class="button">${i18n.site.ok}</button></span>`,
    class: 'alert',
    modal: true,
    noCloseButton: true,
    noClickAway: true,
    show: true,
    actions: { selector: 'button', result: 'ok' },
  });
}

export async function alerts(msgs: string[]): Promise<void> {
  for (const msg of msgs) await alert(msg);
}

export async function info(msg: string, autoDismiss?: Millis): Promise<Dialog> {
  const dlg = await domDialog({
    htmlText: escapeHtmlAddBreaks(msg),
    noCloseButton: true,
  });
  if (!!autoDismiss) setTimeout(() => dlg.close(), autoDismiss);
  return dlg.show();
}

// non-blocking window.confirm-alike
export async function confirm(
  msg: string,
  ok: string = i18n.site.ok,
  cancel: string = i18n.site.cancel,
): Promise<boolean> {
  return (
    (
      await domDialog({
        htmlText: $html`<div>${escapeHtmlAddBreaks(msg)}</div>
          <span><button class="button button-empty cancel">${cancel}</button>
          <button class="button ok">${ok}</button></span>`,
        class: 'alert',
        noCloseButton: true,
        noClickAway: true,
        modal: true,
        show: true,
        focus: '.ok',
        actions: [
          { selector: '.cancel', result: 'cancel' },
          { selector: '.ok', result: 'ok' },
        ],
      })
    ).returnValue === 'ok'
  );
}

// non-blocking window.prompt-alike
export async function prompt(
  msg: string,
  def: string = '',
  valid: (text: string) => boolean = () => true,
): Promise<string | null> {
  const res = await domDialog({
    htmlText: $html`<div>${escapeHtmlAddBreaks(msg)}</div>
      <input type="text"${valid(def) ? '' : ' class="invalid"'} value="${escapeHtml(def)}">
      <span>
        <button class="button button-empty cancel">${i18n.site.cancel}</button>
        <button class="button ok${valid(def) ? '"' : ' disabled" disabled'}>${i18n.site.ok}</button>
      </span>`,
    class: 'alert',
    noCloseButton: true,
    noClickAway: true,
    modal: true,
    show: true,
    focus: 'input',
    actions: [
      { selector: '.ok', result: 'ok' },
      { selector: '.cancel', result: 'cancel' },
      {
        selector: 'input',
        event: 'keydown',
        listener: (e: KeyboardEvent, dlg) => {
          if (e.key !== 'Enter' && e.key !== 'Escape') return;
          e.preventDefault();
          if (e.key === 'Enter' && valid(dlg.view.querySelector<HTMLInputElement>('input')!.value))
            dlg.close('ok');
          else if (e.key === 'Escape') dlg.close('cancel');
        },
      },
      {
        selector: 'input',
        event: 'input',
        listener: (e, dlg) => {
          if (!(e.target instanceof HTMLInputElement)) return;
          const ok = dlg.view.querySelector<HTMLButtonElement>('.ok')!;
          const invalid = !valid(e.target.value);
          e.target.classList.toggle('invalid', invalid);
          ok.classList.toggle('disabled', invalid);
          ok.disabled = invalid;
        },
      },
    ],
  });
  return res.returnValue === 'ok' ? res.view.querySelector('input')!.value : null;
}

export async function choose(
  msg: string,
  options: string[],
  initial?: string,
  mustChoose = false,
): Promise<string | undefined> {
  const res = await domDialog({
    htmlText:
      $html`
      <div>${escapeHtmlAddBreaks(msg)}</div>
      <select ${initial ? 'value="' + initial + '"' : ''}>` +
      options.map(
        option => $html`
          <option value="${escapeHtml(option)}"${option === initial ? ' selected' : ''}>
            ${escapeHtml(option)}
          </option>`,
      ) +
      $html`
      </select>
      <span>` +
      (mustChoose
        ? ''
        : $html`
        <button class="button button-empty cancel">${i18n.site.cancel}</button>`) +
      $html`
        <button class="button ok">${i18n.site.ok}</button>
      </span>`,
    class: 'alert',
    noCloseButton: mustChoose,
    noClickAway: true,
    modal: true,
    show: true,
    actions: [
      {
        selector: '.ok',
        listener: (_, dlg) => dlg.close(dlg.view.querySelector<HTMLSelectElement>('select')?.value),
      },
      { selector: '.cancel', result: 'cancel' },
    ],
  });
  return res.returnValue === 'cancel' ? undefined : res.returnValue;
}

export const makeLinkPopups = (dom: HTMLElement | Cash, selector = 'a[href^="http"]'): void => {
  const $el = $(dom);
  if (!$el.hasClass('link-popup-ready'))
    $el.addClass('link-popup-ready').on('click', selector, function (this: HTMLLinkElement) {
      return onClick(this);
    });
};

const onClick = (a: HTMLLinkElement): boolean => {
  const url = new URL(a.href);
  if (isPassList(url)) return true;

  domDialog({
    class: 'link-popup',
    css: [{ hashed: 'bits.linkPopup' }],
    htmlText: $html`
      <div class="link-popup__content">
        <div class="link-popup__content__title">
          <h2>${i18n.site.youAreLeavingLichess}</h2>
          <p class="link-popup__content__advice">${i18n.site.neverTypeYourPassword}</p>
        </div>
      </div>
      <div class="link-popup__actions">
        <button class="cancel button-link" type="button">${i18n.site.cancel}</button>
        <a href="${a.href}" target="_blank" class="button button-red button-no-upper">
          ${i18n.site.proceedToX(url.host)}
        </a>
      </div>`,
    modal: true,
  }).then(dlg => {
    $('.cancel', dlg.view).on('click', dlg.close);
    $('a', dlg.view).on('click', () => setTimeout(dlg.close, 1000));
    dlg.show();
  });
  return false;
};

const isPassList = (url: URL) => passList().find(h => h === url.host || url.host.endsWith('.' + h));

const passList = () =>
  `lichess.org lichess4545.com ligacatur.com
github.com discord.com discord.gg mastodon.online
bsky.app facebook.com twitch.tv
wikipedia.org wikimedia.org
chess24.com chess.com chessable.com
lc0.org lczero.org stockfishchess.org
`.split(/[ \n]/);

function escapeHtmlAddBreaks(s: string) {
  return escapeHtml(s).replace(/\n/g, '<br>');
}
