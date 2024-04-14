import extendTablesortNumber from 'common/tablesortNumber';
import tablesort from 'tablesort';
import debounce from 'common/debounce';
import { formToXhr } from 'common/xhr';
import { checkBoxAll, expandCheckboxZone, shiftClickCheckboxRange } from './checkBoxes';

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
    .on('keydown', e => {
      if (e.code === 'Enter') form.submit();
    });
};

const setupTable = () => {
  const table = document.querySelector('table.game-list') as HTMLTableElement;
  extendTablesortNumber();
  tablesort(table, { descending: true });

  expandCheckboxZone(table, 'td:first-child', shiftClickCheckboxRange(table));
  checkBoxAll(table);
};

const setupActionForm = () => {
  const form = document.querySelector('.mod-games__analysis-form') as HTMLFormElement;
  const debouncedSubmit = debounce(
    () =>
      formToXhr(form).then(() => {
        const reload = confirm('Analysis completed. Reload the page?');
        if (reload) site.reload();
      }),
    1000,
  );
  $(form).on('click', 'button', (e: Event) => {
    const button = e.target as HTMLButtonElement;
    const action = button.getAttribute('value');
    const nbSelected = form.querySelectorAll('input:checked').length;
    if (nbSelected < 1) return false;
    if (action == 'analyse') {
      if (nbSelected >= 20 && !confirm(`Analyse ${nbSelected} games?`)) return;
      $(form).find('button[value="analyse"]').text('Sent').prop('disabled', true);
      debouncedSubmit();
      return false;
    }
    return;
  });
};
