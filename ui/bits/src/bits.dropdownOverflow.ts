import { frag } from 'lib';
import { json as xhrJson } from 'lib/xhr';
import { isTouchDevice } from 'lib/device';

type HttpMethod = 'GET' | 'POST';

type Menu = {
  items: MenuItem[];
  moreLabel: string;
};

type MenuItem = {
  label: string;
  icon: string;
  href: string;
  category?: string;
  cssClass?: string;
  httpMethod?: HttpMethod;
};

site.load.then(() => {
  const containers = Array.from(document.querySelectorAll<HTMLElement>('.dropdown-overflow'));
  if (containers.length === 0) {
    return;
  }

  function render() {
    containers.forEach(container => {
      renderMenu(container);
      listenToReload(container);
    });
  }

  render();
  window.addEventListener('resize', render);
});

function listenToReload(container: HTMLElement): void {
  container.addEventListener('reload', (e: CustomEvent) => {
    xhrJson(e.detail, { method: 'post', headers: { 'Content-Type': 'application/json' } }).then(menuItems =>
      replaceMenuItems(container, menuItems),
    );
  });
}

function replaceMenuItems(container: HTMLElement, items: MenuItem[]): void {
  const menu = getMenuFromDataAttr(container);
  const newMenuItemsByCategory: Record<string, MenuItem[]> = items.reduce(
    (acc, item) => {
      if (!acc[item.category ?? '']) {
        acc[item.category ?? ''] = [];
      }
      acc[item.category ?? ''].push(item);
      return acc;
    },
    {} as Record<string, MenuItem[]>,
  );

  for (const [category, items] of Object.entries(newMenuItemsByCategory)) {
    const categoryIndex = menu.items.findIndex(item => item.category === category);
    if (categoryIndex === -1) {
      menu.items.push(...items);
    } else {
      menu.items = menu.items.filter(item => item.category !== category);
      menu.items.splice(categoryIndex, 0, ...items);
    }
  }

  setMenuToDataAttr(container, menu);
  renderMenu(container);
}

function renderMenu(container: HTMLElement): void {
  // Remove all children
  container.innerHTML = '';

  const { items, moreLabel } = getMenuFromDataAttr(container);
  const initialWidth = container.offsetWidth;

  const menuContainer = document.createElement('div');
  menuContainer.classList = 'menu-container btn-rack';
  container.appendChild(menuContainer);

  const dropdownDiv = document.createElement('div');
  dropdownDiv.classList = 'dropdown btn-rack__btn';
  menuContainer.appendChild(dropdownDiv);

  const moreButton = document.createElement('a');
  moreButton.textContent = `${moreLabel} ▾`;
  dropdownDiv.appendChild(moreButton);

  const createMenuButton = (className: string, item: MenuItem): HTMLElement => {
    if (item.httpMethod === 'POST') {
      return frag($html`
        <form method="POST" action="${item.href}">
          <button type="submit" class="button-text" data-icon="${item.icon}"}>
            ${item.label}
          </button>
        </form>`);
    }
    const button = document.createElement('a');
    button.className = className;
    if (item.cssClass) {
      button.classList.add(item.cssClass);
    }
    button.textContent = item.label;
    button.href = item.href;
    button.setAttribute('data-icon', item.icon);

    return button;
  };

  let displayedItemCount = 0;

  for (const item of items) {
    const button = createMenuButton('btn-rack__btn', item);
    menuContainer.insertBefore(button, dropdownDiv);

    if (container.offsetWidth > initialWidth && !site.blindMode) {
      menuContainer.removeChild(button);
      break;
    }

    displayedItemCount++;
  }

  if (displayedItemCount < items.length) {
    if (displayedItemCount === 0) {
      menuContainer.classList.remove('btn-rack');
      dropdownDiv.classList.remove('btn-rack__btn');
      moreButton.textContent = '';
      moreButton.setAttribute('data-icon', ''); // Hamburger icon
    }

    const dropdownWindow = document.createElement('div');
    dropdownWindow.className = 'dropdown-window';
    dropdownDiv.appendChild(dropdownWindow);

    // Allow the More button to be focusable for screen readers
    dropdownDiv.tabIndex = 0;
    dropdownDiv.role = 'button';

    const closeListener = (e: Event) => dropdownDiv.contains(e.target as Node) || showDropdownWindow(false);

    const showDropdownWindow = (show?: boolean) => {
      if (show ?? !dropdownDiv.classList.contains('visible'))
        document.addEventListener('click', closeListener);
      else document.removeEventListener('click', closeListener);
      dropdownDiv.classList.toggle('visible', show);
    };

    if (isTouchDevice() && !site.blindMode) dropdownDiv.onclick = () => showDropdownWindow();

    for (let i = displayedItemCount; i < items.length; i++) {
      const button = createMenuButton('text', items[i]);
      dropdownWindow.appendChild(button);
    }
  } else {
    menuContainer.removeChild(dropdownDiv);
  }
}

function getMenuFromDataAttr(root: HTMLElement): Menu {
  return $(root).data('menu');
}

function setMenuToDataAttr(root: HTMLElement, menu: Menu): void {
  $(root).data('menu', menu);
}
