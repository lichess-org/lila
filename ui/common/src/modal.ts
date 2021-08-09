import { h, VNode } from 'snabbdom';
import { bind, MaybeVNodes, onInsert } from './snabbdom';

interface BaseModal {
  class?: string;
  onInsert?: ($wrap: Cash) => void;
  onClose?(): void;
  noClickAway?: boolean;
}

interface Modal extends BaseModal {
  content: Cash;
}

interface SnabModal extends BaseModal {
  content: MaybeVNodes;
}

export default function modal(opts: Modal) {
  modal.close();
  const $wrap = $(
    '<div id="modal-wrap"><span class="close" role="button" aria-label="Close" data-icon="" tabindex="0"></span></div>'
  );
  const $overlay = $(`<div id="modal-overlay" class="${opts.class}">`);
  if (!opts.noClickAway) $overlay.on('click', modal.close);
  $('<a href="#"></a>').appendTo($overlay); // guard against focus escaping to window chrome
  $wrap.appendTo($overlay);
  $('<a href="#"></a>').appendTo($overlay); // guard against focus escaping to window chrome
  opts.content.clone().removeClass('none').appendTo($wrap);
  opts.onInsert && opts.onInsert($wrap);
  modal.onClose = opts.onClose;
  $wrap.find('.close').each(function (this: HTMLElement) {
    bindClose(this, modal.close);
  });
  $('body').addClass('overlayed').prepend($overlay);
  bindWrap($wrap);
  return $wrap;
}

modal.close = () => {
  $('body').removeClass('overlayed');
  $('#modal-overlay').each(function (this: HTMLElement) {
    if (modal.onClose) modal.onClose();
    $(this).remove();
  });
  delete modal.onClose;
};

modal.onClose = undefined as (() => void) | undefined;

export function snabModal(opts: SnabModal): VNode {
  const close = opts.onClose!;
  return h(
    'div#modal-overlay',
    {
      ...(opts.onClose && !opts.noClickAway ? { hook: bind('click', close) } : {}),
    },
    [
      h(
        'div#modal-wrap.' + opts.class,
        {
          hook: onInsert(el => {
            bindWrap($(el));
            opts.onInsert && opts.onInsert($(el));
          }),
        },
        [
          h('span.close', {
            attrs: {
              'data-icon': '',
              role: 'button',
              'aria-label': 'Close',
              tabindex: '0',
            },
            hook: onInsert(el => bindClose(el, close)),
          }),
          h('div', opts.content),
        ]
      ),
    ]
  );
}

const bindClose = (el: HTMLElement, close: () => void) => {
  el.addEventListener('click', close);
  el.addEventListener('keydown', e => (e.code === 'Enter' || e.code === 'Space' ? close() : true));
};

const bindWrap = ($wrap: Cash) => {
  $wrap.on('click', (e: Event) => e.stopPropagation());
  focusFirstChild($wrap);
};

const focusableSelectors =
  'button:not(:disabled), [href], input:not(:disabled):not([type="hidden"]), select:not(:disabled), textarea:not(:disabled), [tabindex="0"]';

export function trapFocus(event: FocusEvent) {
  const wrap: HTMLElement | undefined = $('#modal-wrap')[0];
  if (!wrap) return;
  const position = wrap.compareDocumentPosition(event.target as HTMLElement);
  if (position & Node.DOCUMENT_POSITION_CONTAINED_BY) return;
  const focusableChildren = $(wrap).find(focusableSelectors);
  const index = position & Node.DOCUMENT_POSITION_FOLLOWING ? 0 : focusableChildren.length - 1;
  focusableChildren.get(index)?.focus();
  event.preventDefault();
}

export const focusFirstChild = (parent: Cash) => {
  const children = parent.find(focusableSelectors);
  // prefer child 1 over child 0 because child 0 should be a close button
  (children[1] ?? children[0])?.focus();
};
