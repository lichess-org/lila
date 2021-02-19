import extendTablesortNumber from './component/tablesort-number';
import tablesort from 'tablesort';

lichess.load.then(() => {
  extendTablesortNumber();
  tablesort(document.querySelector('table.game-list'), { descending: true });
});
