type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'OPTIONS' | 'HEAD';

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
    containers.forEach(container => renderMenu(container));
  }

  render();
  window.addEventListener('resize', render);
});

export function replaceMenuItems(container: HTMLElement, items: MenuItem[]): void {
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
  menuContainer.className = 'menu-container';
  menuContainer.classList.add('btn-rack');
  container.appendChild(menuContainer);

  const dropdownDiv = document.createElement('div');
  dropdownDiv.className = 'dropdown';
  dropdownDiv.classList.add('btn-rack__btn');
  menuContainer.appendChild(dropdownDiv);

  const moreButton = document.createElement('a');
  moreButton.textContent = `${moreLabel} ▾`;
  dropdownDiv.appendChild(moreButton);

  const createMenuButton = (className: string, item: MenuItem): HTMLAnchorElement => {
    const button = document.createElement('a');
    button.className = className;
    if (item.cssClass) {
      button.classList.add(item.cssClass);
    }
    button.textContent = item.label;
    button.href = item.href;
    button.setAttribute('data-icon', item.icon);

    if (item.httpMethod && item.httpMethod !== 'GET') {
      button.href = window.location.href;
      button.onclick = () => {
        fetch(item.href, {
          method: item.httpMethod,
        });
      };
    }

    return button;
  };

  let displayedItemCount = 0;

  for (const item of items) {
    const button = createMenuButton('btn-rack__btn', item);
    menuContainer.insertBefore(button, dropdownDiv);

    if (container.offsetWidth > initialWidth) {
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

    const showDropdownWindow = () => {
      dropdownWindow.style.visibility = 'visible';
    };

    dropdownDiv.onclick = showDropdownWindow;
    dropdownDiv.onkeydown = event => {
      if (event.key === 'Enter') {
        showDropdownWindow();
      }
    };

    dropdownWindow.addEventListener('focusout', () => {
      setTimeout(() => {
        if (!dropdownWindow.contains(document.activeElement)) {
          dropdownWindow.style.visibility = 'hidden';
        }
      }, 0);
    });

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
