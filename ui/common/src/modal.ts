export default function modal(content: Cash, cls?: string, onClose?: () => void) {
  modal.close();
  const $wrap: any = $('<div id="modal-wrap">')
    .html(content.clone().removeClass('none').html())
    .prepend('<span class="close" data-icon="L"></span>'),
  $overlay = $('<div id="modal-overlay">')
    .addClass(cls || '')
    .html($wrap);
  modal.onClose = onClose;
  $wrap.find('.close').on('click', modal.close);
  $overlay.on('click', function() {
    // disgusting hack
    // dragging slider out of a modal closes the modal
    if (!$('.ui-slider-handle.ui-state-focus').length) modal.close();
  });
  $wrap.on('click', (e: Event) => e.stopPropagation());
  $('body').addClass('overlayed').prepend($overlay);
  return $wrap;
};
modal.close = () => {
  $('body').removeClass('overlayed');
  $('#modal-overlay').each(function(this: HTMLElement) {
    if (modal.onClose) modal.onClose();
    $(this).remove();
  });
  delete modal.onClose;
};
modal.onClose = undefined as (() => void) | undefined;
