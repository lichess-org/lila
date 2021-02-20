import extendTablesortNumber from './component/tablesort-number';
import tablesort from 'tablesort';
import debounce from 'common/debounce';
import { formToXhr } from 'common/xhr';

type OnSelect = (input: HTMLInputElement, shift: boolean) => void;

lichess.load.then(() => {
  const form = document.querySelector('.mod-games__form') as HTMLFormElement;
  const table = document.querySelector('table.game-list') as HTMLTableElement;
  extendTablesortNumber();
  tablesort(table, { descending: true });

  const onSelect = shiftClickCheckboxRange(table);

  expandCheckboxZone(table, onSelect);

  checkBoxAll(table);

  bindForm(form);
});

const bindForm = (form: HTMLFormElement) => {
  $(form).on(
    'submit',
    debounce(() => formToXhr(form).then(() => alert('Analysis started.')), 1000)
  );
};

const shiftClickCheckboxRange = (table: HTMLTableElement): OnSelect => {
  let lastChecked: HTMLInputElement | undefined;

  const checkIntermediateBoxes = (first: HTMLInputElement, last: HTMLInputElement) => {
    let started = false;
    for (const input of table.querySelectorAll('tbody input')) {
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

const expandCheckboxZone = (table: HTMLTableElement, onSelect: OnSelect) =>
  $(table).on('click', 'td:first-child', (e: MouseEvent) => {
    if ((e.target as HTMLElement).tagName == 'INPUT') onSelect(e.target as HTMLInputElement, e.shiftKey);
    else {
      const input = (e.target as HTMLTableDataCellElement).querySelector('input') as HTMLInputElement;
      input.checked = !input.checked;
      onSelect(input, e.shiftKey);
    }
  });

const checkBoxAll = (table: HTMLTableElement) =>
  $(table)
    .find('thead input')
    .on('change', (e: MouseEvent) =>
      $(table)
        .find('tbody input')
        .prop('checked', (e.target as HTMLInputElement).checked)
    );
