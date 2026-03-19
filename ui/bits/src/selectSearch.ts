export function createSelectSearch(dropdown: HTMLSelectElement): void {
  const wrapper = document.createElement('div');
  wrapper.className = 'select-search';

  const items: { el: HTMLElement; value: string; label: string }[] = [];
  let highlightIndex = -1;

  const input = document.createElement('input');
  input.type = 'text';
  input.className = 'select-search__input';
  input.autocomplete = 'off';
  input.spellcheck = false;
  input.placeholder = selectedLabel() || 'Search...';

  const list = document.createElement('div');
  list.className = 'select-search__list';

  for (const opt of dropdown.options) {
    const label = opt.textContent?.trim() || '';
    const item = document.createElement('div');
    item.className = 'select-search__item';
    item.textContent = label;
    if (opt.value === dropdown.value) item.classList.add('active');
    item.addEventListener('mousedown', () => selectOption(opt.value, label));
    list.appendChild(item);
    items.push({ el: item, value: opt.value, label });
  }

  dropdown.style.display = 'none';
  dropdown.insertAdjacentElement('afterend', wrapper);
  wrapper.appendChild(input);
  wrapper.appendChild(list);

  input.addEventListener('focus', () => {
    wrapper.classList.add('open');
    input.value = '';
    filter('');
    const visible = visibleItems();
    highlightIndex = visible.findIndex(i => i.value === dropdown.value);
    setHighlight();
    if (highlightIndex >= 0) visible[highlightIndex].el.scrollIntoView({ block: 'nearest' });
  });

  input.addEventListener('input', () => {
    filter(input.value);
    highlightIndex = -1;
    setHighlight();
  });

  input.addEventListener('keydown', (e: KeyboardEvent) => {
    const visible = visibleItems();
    const delta = e.key === 'ArrowDown' ? 1 : e.key === 'ArrowUp' ? -1 : 0;
    if (e.key === 'Escape') input.blur();
    else if (delta && visible.length) {
      e.preventDefault();
      highlightIndex = (highlightIndex + delta + visible.length) % visible.length;
      setHighlight();
      visible[highlightIndex].el.scrollIntoView({ block: 'nearest' });
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const target = visible[Math.max(0, highlightIndex)];
      if (target) selectOption(target.value, target.label);
    }
  });

  input.addEventListener('blur', () => {
    wrapper.classList.remove('open');
    input.value = selectedLabel() || '';
  });

  function visibleItems() {
    return items.filter(i => !i.el.classList.contains('none'));
  }

  function setHighlight() {
    const visible = visibleItems();
    items.forEach(i => i.el.classList.remove('highlight'));
    if (highlightIndex >= 0 && visible[highlightIndex]) visible[highlightIndex].el.classList.add('highlight');
  }

  function selectedLabel(): string {
    return items.find(i => i.value === dropdown.value)?.label || '';
  }

  function selectOption(value: string, label: string) {
    dropdown.value = value;
    dropdown.dispatchEvent(new Event('change'));
    input.value = label;
    input.blur();
    items.forEach(i => i.el.classList.toggle('active', i.value === value));
  }

  function filter(query: string) {
    const q = query.toLowerCase();
    items.forEach(i =>
      i.el.classList.toggle(
        'none',
        !!q && !i.label.toLowerCase().includes(q) && !i.value.toLowerCase().includes(q),
      ),
    );
  }

  input.value = selectedLabel();
}
