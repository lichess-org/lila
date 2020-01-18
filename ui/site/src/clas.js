var tablesort = require('tablesort');

$(function() {
  $('table.sortable').each(function() {
    tablesort(this, {});
  });
  $('.name-regen').click(function() {
    $.get($(this).attr('href'), name => $('#form3-create-username').val(name));
    return false;
  });
});
