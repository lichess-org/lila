export default function flairPickerLoader(element: HTMLElement): void {
  const parent = $(element).parent();
  const close = () => element.removeAttribute('open');
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
  $(element).on('toggle', () =>
    site.asset.loadEsm('bits.flairPicker', {
      init: {
        element: element.querySelector('.flair-picker')!,
        close,
        onEmojiSelect,
      },
    }),
  );
}
