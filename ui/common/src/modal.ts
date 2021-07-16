export default function modal(content: Cash, cls?: string, onClose?: () => void) {
  modal.close();
  const $wrap = $(
    '<div id="modal-wrap"><span class="close" role="button" aria-label="Close" data-icon="" tabindex="0"></span></div>'
  );
  const $overlay = $(`<div id="modal-overlay" class="${cls}">`).on('click', modal.close);
  $('<a href="#"></a>').appendTo($overlay); // guard against focus escaping to window chrome
  $wrap.appendTo($overlay);
  $('<a href="#"></a>').appendTo($overlay); // guard against focus escaping to window chrome
  content.clone().removeClass('none').appendTo($wrap);
  modal.onClose = onClose;
  $wrap
    .find('.close')
    .on('click', modal.close)
    .on('keydown', (e: KeyboardEvent) => (e.code === 'Space' || e.code === 'Enter' ? modal.close() : true));
  $wrap.on('click', (e: Event) => e.stopPropagation());
  $('body').addClass('overlayed').prepend($overlay);
  focusFirstChild($wrap);
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

const focusableSelectors =
  'button:not(:disabled), [href], input:not(:disabled):not([type="hidden"]), select:not(:disabled), textarea:not(:disabled), [tabindex="0"]';

export function trapFocus(event: FocusEvent) {
  const wrap: HTMLElement | undefined = $('#modal-wrap').get(0);
  console.log(wrap);
  if (!wrap) return;
  const position = wrap.compareDocumentPosition(event.target as HTMLElement);
  if (position & Node.DOCUMENT_POSITION_CONTAINED_BY) return;
  const focusableChildren = $(wrap).find(focusableSelectors);
  const index = position & Node.DOCUMENT_POSITION_FOLLOWING ? 0 : focusableChildren.length - 1;
  focusableChildren.get(index)?.focus();
  event.preventDefault();
}

export function focusFirstChild(parent: Cash) {
  const children = parent.find(focusableSelectors);
  // prefer child 1 over child 0 because child 0 should be a close button
  (children.get(1) ?? children.get(0))?.focus();
}
