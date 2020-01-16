var tablesort = require('tablesort');

$(function() {
  $('table.sortable').each(function() {
    tablesort(this, {});
  });
});
