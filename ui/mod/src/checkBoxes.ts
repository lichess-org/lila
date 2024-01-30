export type OnSelect = (input: HTMLInputElement, shift: boolean) => void;

export const shiftClickCheckboxRange = (table: HTMLTableElement): OnSelect => {
  let lastChecked: HTMLInputElement | undefined;

  const checkIntermediateBoxes = (first: HTMLInputElement, last: HTMLInputElement) => {
    let started = false;
    for (const input of table.querySelectorAll('tbody tr:not(.none) input:not(:disabled)')) {
      if (first == input || last == input) {
        if (started) return;
        started = true;
      } else if (started) (input as HTMLInputElement).checked = last.checked;
    }
  };

  return (input: HTMLInputElement, shift: boolean) => {
    if (shift && lastChecked && input != lastChecked) checkIntermediateBoxes(lastChecked, input);
    lastChecked = input;
  };
};

export const expandCheckboxZone = (table: HTMLTableElement, tdSelector: string, onSelect: OnSelect) =>
  $(table).on('click', tdSelector, (e: MouseEvent) => {
    if ((e.target as HTMLElement).tagName == 'INPUT') onSelect(e.target as HTMLInputElement, e.shiftKey);
    else {
      const input = (e.target as HTMLTableElement).querySelector('input') as HTMLInputElement | undefined;
      if (input && !input.disabled) {
        input.checked = !input.checked;
        onSelect(input, e.shiftKey);
      }
    }
  });

export const checkBoxAll = (table: HTMLTableElement) =>
  $(table)
    .find('thead input')
    .on('change', (e: MouseEvent) =>
      $(table)
        .find('tbody input:not(:disabled)')
        .prop('checked', (e.target as HTMLInputElement).checked),
    );

export const selector =
  (table: HTMLTableElement, select: HTMLSelectElement) => (f: (action: string) => void) =>
    $(select).on('change', _ => {
      const action = select.value;
      if (action) {
        select.value = '';
        if (action == 'all' || action == 'none')
          $(table)
            .find('tbody tr:not(.none) input:not(:disabled)')
            .prop('checked', action == 'all');
        else f(action);
      }
      return false;
    });
