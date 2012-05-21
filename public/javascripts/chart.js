if (typeof google.visualization == "undefined" ) {
  google.load("visualization", "1", {packages:["corechart"]});
}

google.elemToData = function(elem) {
  var data = new google.visualization.DataTable();
  $.each($(elem).data('columns'), function() {
    data.addColumn(this[0], this[1]);
  });
  data.addRows($(elem).data('rows'));

  return data;
}
