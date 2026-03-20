export function createSelectSearch(select: HTMLSelectElement): void {
  const options = [...select.options];

  const container = document.createElement('div');
  container.classList.add('select-search');
  select.insertAdjacentElement('afterend', container);

  const toggle = document.createElement('div');
  toggle.classList.add('select-search__toggle');
  toggle.textContent = select.selectedOptions[0]?.textContent ?? options[0]?.textContent ?? '';
  container.appendChild(toggle);

  const menu = document.createElement('div');
  menu.classList.add('select-search__menu');
  container.appendChild(menu);

  const search = document.createElement('input');
  search.type = 'text';
  search.placeholder = 'Search...';
  search.classList.add('select-search__input');
  menu.appendChild(search);

  const list = document.createElement('div');
  list.classList.add('select-search__list');
  menu.appendChild(list);

  for (const option of options) {
    const item = document.createElement('div');
    item.classList.add('select-search__item');
    item.dataset.value = option.value;
    item.textContent = option.textContent;
    if (option.value === select.value) item.classList.add('selected');
    item.addEventListener('click', () => {
      toggle.textContent = item.textContent;
      select.value = item.dataset.value!;
      select.dispatchEvent(new Event('change'));
      list.querySelector('.selected')?.classList.remove('selected');
      item.classList.add('selected');
      closeMenu();
    });
    list.appendChild(item);
  }

  toggle.addEventListener('click', () => {
    const isOpen = container.classList.toggle('open');
    if (isOpen) {
      search.focus();
      list.querySelector('.selected')?.scrollIntoView({ block: 'nearest' });
    }
  });

  search.addEventListener('input', () => {
    const query = search.value.toLowerCase();
    for (const item of list.children) {
      const el = item as HTMLElement;
      const text = (el.textContent ?? '').toLowerCase();
      el.classList.toggle('none', !text.includes(query));
    }
  });

  search.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key === 'Escape') closeMenu();
    else if (e.key === 'Enter') {
      e.preventDefault();
      const visible = list.querySelector<HTMLElement>('.select-search__item:not(.none)');
      visible?.click();
    }
  });

  document.addEventListener('click', (e: MouseEvent) => {
    if (!container.contains(e.target as Node)) closeMenu();
  });

  function closeMenu() {
    container.classList.remove('open');
    search.value = '';
    for (const item of list.children) (item as HTMLElement).classList.remove('none');
  }

  select.classList.add('none');
}
