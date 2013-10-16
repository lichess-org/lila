function drawCharts() {

  var light = $('body').hasClass('light');
  var bg = "transparent";
  var textcolor = {
    color: light ? '#848484' : '#a0a0a0'
  };
  var weak = light ? '#ccc' : '#404040';
  var strong = light ? '#a0a0a0' : '#606060';
  var lineColor = {
    color: weak
  };

  function elemToData(elem) {
    var data = new google.visualization.DataTable();
    $.each($(elem).data('columns'), function() {
      data.addColumn(this[0], this[1]);
    });
    data.addRows(_.map($(elem).data('rows'), function(e) {
      var a = _.map(_.rest(e), parseFloat);
      a.unshift(_.first(e));
      return a;
    }));

    return data;
  }

  $('div.move-time').each(function() {
    var data = elemToData(this);
    var chart = new google.visualization.AreaChart(this);
    chart.draw(data, {
      width: 747,
      height: 400,
      title: $(this).data('title'),
      titleTextStyle: textcolor,
      chartArea: {
        left: "5%",
        top: "5%",
        width: "78%",
        height: "90%"
      },
      backgroundColor: bg,
      vAxis: {
        textStyle: textcolor,
        gridlines: lineColor
      },
      legend: {
        textStyle: textcolor
      }
    });
  });

  $('div.move-times').each(function() {
    var data = elemToData(this);
    var chart = new google.visualization.PieChart(this);
    chart.draw(data, {
      width: 366,
      height: 300,
      title: $(this).data('title'),
      legend: {
        textStyle: textcolor
      },
      titleTextStyle: textcolor,
      chartArea: {
        left: "0%",
        width: "100%",
        height: "80%"
      },
      is3D: true,
      backgroundColor: bg
    });
  });
}

$(function() {
  $.getScript('https://www.google.com/jsapi?autoload={"modules":[{"name":"visualization","version":"1","packages":["corechart"],"callback":"drawCharts"}]}');
});
