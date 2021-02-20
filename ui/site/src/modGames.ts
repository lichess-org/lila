import extendTablesortNumber from './component/tablesort-number';
import tablesort from 'tablesort';

type OnSelect = (input: HTMLInputElement, shift: boolean) => void;

lichess.load.then(() => {
  const table = document.querySelector('table.game-list') as HTMLTableElement;
  extendTablesortNumber();
  tablesort(table, { descending: true });

  const onSelect = shiftClickCheckboxRange(table);

  $(table).on('click', 'input', (event: MouseEvent) => onSelect(event.target as HTMLInputElement, event.shiftKey));

  expandCheckboxZone(table, onSelect);
});

const shiftClickCheckboxRange = (table: HTMLTableElement): OnSelect => {
  let lastChecked: HTMLInputElement | undefined;

  const checkIntermediateBoxes = (first: HTMLInputElement, last: HTMLInputElement) => {
    let started = false;
    for (const input of table.querySelectorAll('input')) {
      if (first == input || last == input) {
        if (started) return;
        started = true;
      } else if (started) input.checked = last.checked;
    }
  };

  return (input: HTMLInputElement, shift: boolean) => {
    if (shift && lastChecked && input != lastChecked) checkIntermediateBoxes(lastChecked, input);
    lastChecked = input;
  };
};

const expandCheckboxZone = (table: HTMLTableElement, onSelect: OnSelect) =>
  $(table).on('click', 'td:first-child', (e: MouseEvent) => {
    const input = (e.target as HTMLTableDataCellElement).querySelector('input') as HTMLInputElement;
    input.checked = !input.checked;
    onSelect(input, e.shiftKey);
  });
