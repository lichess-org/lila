export default function modal(content: Cash, cls?: string, onClose?: () => void) {
  modal.close();
  const $wrap: any = $('<div id="modal-wrap"><span class="close" data-icon="L"></span></div>');
  const $overlay = $(`<div id="modal-overlay" class="${cls}">`).on('click', modal.close);
  $wrap.appendTo($overlay);
  content.clone().removeClass('none').appendTo($wrap);
  modal.onClose = onClose;
  $wrap.find('.close').on('click', modal.close);
  $wrap.on('click', (e: Event) => e.stopPropagation());
  $('body').addClass('overlayed').prepend($overlay);
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
