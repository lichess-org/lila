export default function modal(html: any, cls?: string, onClose?: () => void) {
  modal.close();
  if (!html.clone) html = $('<div>' + html + '</div>');
  const $wrap: any = $('<div id="modal-wrap">')
    .html(html.clone().removeClass('none'))
    .prepend('<span class="close" data-icon="L"></span>'),
  $overlay = $('<div id="modal-overlay">')
    .addClass(cls || '')
    .data('onClose', onClose)
    .html($wrap);
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
    ($(this).data('onClose') || $.noop)();
    $(this).remove();
  });
};
