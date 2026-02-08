import * as licon from 'lib/licon';
import { writeTextClipboard, text as xhrText } from 'lib/xhr';
import topBar from './topBar';
import { userComplete } from 'lib/view/userComplete';
import { confirm } from 'lib/view';

export function addWindowHandlers() {
  let animFrame: number;

  window.addEventListener('resize', () => {
    cancelAnimationFrame(animFrame);
    animFrame = requestAnimationFrame(setViewportHeight);
  });

  // ios safari vh correction
  function setViewportHeight() {
    document.body.style.setProperty('---viewport-height', `${window.innerHeight}px`);
  }
}

export function addDomHandlers() {
  topBar();

  $('#main-wrap').on('click', '.copy-me__button', function (this: HTMLElement) {
    const showCheckmark = () => {
      $(this).attr('data-icon', licon.Checkmark).removeClass('button-metal');
      setTimeout(() => $(this).attr('data-icon', licon.Clipboard).addClass('button-metal'), 1000);
    };
    const fetchContent = $(this).parent().hasClass('fetch-content');
    $(this.parentElement!.firstElementChild!).each(function (this: any) {
      try {
        if (fetchContent) writeTextClipboard(this.href, showCheckmark);
        else navigator.clipboard.writeText(this.value || this.href).then(showCheckmark);
      } catch (e) {
        console.error(e);
      }
    });
    return false;
  });

  $('body').on('click', '.relation-button', function (this: HTMLAnchorElement) {
    const $a = $(this).addClass('processing').css('opacity', 0.3);
    const dropdownOverflowParent = this.closest<HTMLElement>('.dropdown-overflow');
    if (dropdownOverflowParent) {
      dropdownOverflowParent.dispatchEvent(new CustomEvent('reload', { detail: this.href }));
    } else {
      xhrText(this.href, { method: 'post' }).then(html => {
        if ($a.hasClass('aclose')) $a.hide();
        else if (html.includes('relation-actions')) $a.parent().replaceWith(html);
        else $a.replaceWith(html);
      });
    }
    return false;
  });

  $('.user-autocomplete').each(function (this: HTMLInputElement) {
    const focus = !!this.autofocus;
    const start = () =>
      userComplete({
        input: this,
        friend: !!this.dataset.friend,
        tag: this.dataset.tag as any,
        focus,
      });

    if (focus) start();
    else $(this).one('focus', start);
  });

  $('#main-wrap').on(
    'click',
    '.yes-no-confirm, .ok-cancel-confirm',
    async function (this: HTMLElement, e: Event) {
      if (!e.isTrusted) return;
      e.preventDefault();
      const [confirmText, cancelText] = this.classList.contains('yes-no-confirm')
        ? [i18n.site.yes, i18n.site.no]
        : [i18n.site.ok, i18n.site.cancel];
      if (await confirm(this.title || 'Confirm this action?', confirmText, cancelText))
        (e.target as HTMLElement)?.click();
    },
  );
}
