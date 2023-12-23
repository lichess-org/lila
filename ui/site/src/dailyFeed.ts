$('.emoji-details').each(function (this: HTMLElement) {
  const details = this;
  const parent = $(details).parent();
  const close = () => details.removeAttribute('open');
  const onEmojiSelect = (i?: { id: string; src: string }) => {
    parent.find('input[name="flair"]').val(i?.id ?? '');
    parent.find('.uflair').remove();
    if (i) parent.find('.flair-container').append('<img class="uflair" src="' + i.src + '" />');
    close();
  };
  parent.find('.emoji-remove').on('click', e => {
    e.preventDefault();
    onEmojiSelect();
    $(e.target).remove();
  });
  $(details).on('toggle', () =>
    lichess.asset.loadEsm('flairPicker', {
      init: {
        element: details.querySelector('.flair-picker')!,
        close,
        onEmojiSelect,
      },
    }),
  );
});
