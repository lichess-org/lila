import { sortTable, extendTablesortNumber } from 'lib/tablesort';
import { debounce } from 'lib/async';
import { formToXhr } from 'lib/xhr';
import { checkBoxAll, expandCheckboxZone, shiftClickCheckboxRange } from './checkBoxes';
import { confirm, enter } from 'lib/view';

site.load.then(() => {
  setupTable();
  setupFilter();
  setupActionForm();
});

const setupFilter = () => {
  const form = document.querySelector('.mod-games__filter-form') as HTMLFormElement;
  $(form)
    .find('select')
    .on('change', () => form.submit());
  $(form)
    .find('input')
    .on(
      'keydown',
      enter(() => form.submit()),
    );
};

const setupTable = () => {
  const table = document.querySelector('table.game-list') as HTMLTableElement;
  extendTablesortNumber();
  sortTable(table, { descending: true });

  expandCheckboxZone(table, 'td:first-child', shiftClickCheckboxRange(table));
  checkBoxAll(table);
};

const setupActionForm = () => {
  const form = document.querySelector('.mod-games__analysis-form') as HTMLFormElement;
  const debouncedSubmit = debounce(
    () =>
      formToXhr(form).then(async () => {
        if (await confirm('Analysis completed. Reload the page?')) site.reload();
      }),
    1000,
  );
  $(form).on('click', 'button', async (e: Event) => {
    const button = e.target as HTMLButtonElement;
    const action = button.getAttribute('value');
    const nbSelected = form.querySelectorAll('input:checked').length;
    if (action !== 'analyse') return;
    e.preventDefault();
    if (nbSelected < 1) return;
    if (nbSelected >= 20 && !(await confirm(`Analyse ${nbSelected} games?`))) return;
    $(form).find('button[value="analyse"]').text('Sent').prop('disabled', true);
    debouncedSubmit();
  });
};
