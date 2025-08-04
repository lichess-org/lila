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

  let displayedItemCount = 0;

  for (const item of items) {
    const button = document.createElement('a');
    button.className = 'btn-rack__btn';
    if (item.cssClass) {
      button.classList.add(item.cssClass);
    }
    button.textContent = item.label;
    button.href = item.href;
    button.setAttribute('data-icon', item.icon);
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
      const button = document.createElement('a');
      button.className = 'text';
      if (items[i].cssClass) {
        button.classList.add(items[i].cssClass!);
      }
      button.textContent = items[i].label;
      button.href = items[i].href;
      button.setAttribute('data-icon', items[i].icon);
      dropdownWindow.appendChild(button);
    }
  } else {
    menuContainer.removeChild(dropdownDiv);
  }
}

function getMenuFromDataAttr(root: HTMLElement): Menu {
  const data = root.getAttribute('data-menu');
  if (!data) {
    return {
      items: [],
      moreLabel: '',
    };
  }

  try {
    return JSON.parse(data);
  } catch {
    return {
      items: [],
      moreLabel: '',
    };
  }
}

function setMenuToDataAttr(root: HTMLElement, menu: Menu): void {
  root.setAttribute('data-menu', JSON.stringify(menu));
}
