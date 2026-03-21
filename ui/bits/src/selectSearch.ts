import { frag } from 'lib';

export function createSelectSearch(select: HTMLSelectElement): void {
  const container = frag<HTMLDivElement>(
    '<div class="select-search">' +
      '<button type="button" class="select-search__toggle" aria-haspopup="listbox" aria-expanded="false"></button>' +
      '<div class="select-search__menu">' +
      '<input type="text" class="select-search__input">' +
      '<div class="select-search__list" role="listbox"></div>' +
      '</div>' +
      '</div>',
  );
  select.insertAdjacentElement('afterend', container);

  const toggle = container.querySelector<HTMLButtonElement>('.select-search__toggle')!;
  if (select.classList.contains('form-control')) toggle.classList.add('form-control');
  toggle.textContent = select.selectedOptions[0]?.textContent ?? select.options[0]?.textContent ?? '';

  const search = container.querySelector<HTMLInputElement>('.select-search__input')!;
  search.placeholder = i18n.site.search;
  search.setAttribute('aria-label', i18n.site.search);

  const list = container.querySelector<HTMLElement>('.select-search__list')!;

  for (const option of select.options) {
    const item = document.createElement('div');
    item.classList.add('select-search__item');
    item.setAttribute('role', 'option');
    item.dataset.value = option.value;
    item.textContent = option.textContent;
    item.setAttribute('aria-selected', String(option.value === select.value));
    option.value === select.value && item.classList.add('selected');
    item.addEventListener('click', () => selectItem(item));
    list.appendChild(item);
  }

  function selectItem(item: HTMLElement) {
    toggle.textContent = item.textContent;
    select.value = item.dataset.value!;
    select.dispatchEvent(new Event('change', { bubbles: true, cancelable: true }));
    list.querySelector('.selected')?.classList.remove('selected');
    Array.from(list.children).forEach(el => el.removeAttribute('aria-selected'));
    item.classList.add('selected');
    item.setAttribute('aria-selected', 'true');
    closeMenu();
    toggle.focus();
  }

  function visibleItems(): HTMLElement[] {
    return [...list.querySelectorAll<HTMLElement>('.select-search__item:not(.none)')];
  }

  toggle.addEventListener('click', () => {
    const wasOpen = container.classList.contains('open');
    if (wasOpen) {
      closeMenu();
      return;
    }
    container.classList.add('open');
    toggle.setAttribute('aria-expanded', 'true');
    search.focus();
    list.querySelector('.selected')?.scrollIntoView({ block: 'nearest' });
  });

  toggle.addEventListener('keydown', (e: KeyboardEvent) => {
    if (['ArrowDown', 'ArrowUp', ' '].includes(e.key) && !container.classList.contains('open')) {
      e.preventDefault();
      toggle.click();
    }
  });

  search.addEventListener('input', () => {
    const query = search.value.toLowerCase();
    Array.from(list.children).forEach(item => {
      item.classList.remove('focus');
      item.classList.toggle('none', !(item.textContent ?? '').toLowerCase().includes(query));
    });
  });

  search.addEventListener('keydown', (e: KeyboardEvent) => {
    const items = visibleItems();
    const focused = list.querySelector<HTMLElement>('.focus');
    if (e.key === 'Escape') {
      closeMenu();
      toggle.focus();
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const target = focused ?? items[0];
      if (target) selectItem(target);
    } else if (['ArrowDown', 'ArrowUp'].includes(e.key)) {
      e.preventDefault();
      if (!items.length) return;
      const current = focused ? items.indexOf(focused) : -1;
      const next =
        e.key === 'ArrowDown'
          ? current < items.length - 1
            ? current + 1
            : 0
          : current > 0
            ? current - 1
            : items.length - 1;
      focused?.classList.remove('focus');
      items[next].classList.add('focus');
      items[next].scrollIntoView({ block: 'nearest' });
    }
  });

  document.addEventListener('click', (e: MouseEvent) => {
    !container.contains(e.target as Node) && closeMenu();
  });

  function closeMenu() {
    container.classList.remove('open');
    toggle.setAttribute('aria-expanded', 'false');
    search.value = '';
    Array.from(list.children).forEach(i => i.classList.remove('none', 'focus'));
  }

  select.classList.add('none');
}
