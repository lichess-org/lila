var tablesort = require('tablesort');

$(function() {
  $('table.sortable').each(function() {
    tablesort(this, {});
  });
  $('.name-regen').click(() => location.reload());
});
