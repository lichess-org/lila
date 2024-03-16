import * as xhr from 'common/xhr';
import extendTablesortNumber from 'common/tablesortNumber';
import tablesort from 'tablesort';
import { checkBoxAll, expandCheckboxZone, selector, shiftClickCheckboxRange } from './checkBoxes';

site.load.then(() => {
  $('.slist, slist-pad')
    .find('.mark-alt')
    .on('click', function (this: HTMLAnchorElement) {
      if (confirm('Close alt account?')) {
        xhr.text(this.getAttribute('href')!, { method: 'post' });
        $(this).remove();
      }
    });

  $('.mod-user-table').each(function (this: HTMLTableElement) {
    const table = this;
    extendTablesortNumber();
    tablesort(table, { descending: true });
    expandCheckboxZone(table, 'td:last-child', shiftClickCheckboxRange(table));
    checkBoxAll(table);
    const select = table.querySelector('thead select');
    if (select)
      selector(
        table,
        select as HTMLSelectElement,
      )(async action => {
        if (action == 'alt') {
          const usernames = Array.from(
            $(table)
              .find('td:last-child input:checked')
              .map((_, input) => $(input).parents('tr').find('td:first-child').data('sort')),
          );
          if (usernames.length > 0 && confirm(`Close ${usernames.length} alt accounts?`)) {
            console.log(usernames);
            await xhr.text('/mod/alt-many', { method: 'post', body: usernames.join(' ') });
            location.reload();
          }
        }
      });
  });
});
