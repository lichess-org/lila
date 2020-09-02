lichess.modal = function(html, cls, onClose) {
  lichess.modal.close();
  if (!html.clone) html = $('<div>' + html + '</div>');
  const $wrap = $('<div id="modal-wrap">')
    .html(html.clone().removeClass('none'))
    .prepend('<span class="close" data-icon="L"></span>'),
  $overlay = $('<div id="modal-overlay">')
    .addClass(cls)
    .data('onClose', onClose)
    .html($wrap);
  $wrap.find('.close').on('click', lichess.modal.close);
  $overlay.on('click', function() {
    // disgusting hack
    // dragging slider out of a modal closes the modal
    if (!$('.ui-slider-handle.ui-state-focus').length) lichess.modal.close();
  });
  $wrap.on('click', function(e) {
    e.stopPropagation();
  });
  $('body').addClass('overlayed').prepend($overlay);
  return $wrap;
};
lichess.modal.close = () => {
  $('body').removeClass('overlayed');
  $('#modal-overlay').each(function() {
    ($(this).data('onClose') || $.noop)();
    $(this).remove();
  });
};
