import { frag } from 'common';

export default async function flairPickerLoader(element: HTMLElement): Promise<void> {
  const selectEl = element.querySelector('select')!;
  const pickerEl = element.querySelector('.flair-picker') as HTMLElement;
  const removeEl = element.querySelector('.emoji-remove') as HTMLElement;
  const isOpen = () => !pickerEl.classList.contains('none');

  const toggle = () => {
    if (isOpen() && (pickerEl.contains(document.activeElement) || document.activeElement === removeEl))
      selectEl.focus();
    pickerEl.classList.toggle('none');
  };

  const onEmojiSelect = (i?: { id: string; src: string }) => {
    element.querySelector('.emoji-popup-button option')?.remove();
    if (i?.id) selectEl.append(frag('<option value="' + i.id + '" selected></option>'));
    element.querySelector<HTMLImageElement>('.emoji-popup-button img')!.src = i?.src ?? '';
    toggle();
  };

  const onClick = async (e: Event) => {
    if (e instanceof KeyboardEvent && e.key !== 'Enter' && e.key !== ' ') return;
    e.preventDefault();
    toggle();
    selectEl.focus();
  };

  await Promise.all([
    site.asset.loadCssPath('bits.flairPicker'),
    site.asset.loadEsm('bits.flairPicker', {
      init: {
        element: element.querySelector('.flair-picker')!,
        onEmojiSelect,
        close: (e: PointerEvent) => {
          if (!isOpen() || selectEl.contains(e.target as Node)) return;
          toggle();
        },
      },
    }),
  ]);

  ['mousedown', 'keydown'].forEach(t => selectEl.addEventListener(t, onClick));
  removeEl.addEventListener('click', e => {
    e.preventDefault();
    onEmojiSelect();
  });

  element.closest('.dialog-content')?.addEventListener('click', (e: PointerEvent) => {
    // em's onClickOutside callback does not trigger inside modal dialog, so do it manually
    if (!isOpen() || [selectEl, pickerEl].some(el => el.contains(e.target as Node))) return;
    e.preventDefault();
    toggle();
  });

  if (!CSS.supports('selector(:has(option))')) {
    // let old browsers set and remove flairs
    element.querySelector('img')!.style.display = 'block';
    element.querySelector<HTMLElement>('.emoji-remove')!.style.display = 'block';
  }
}
