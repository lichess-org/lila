function drawCharts() {

  var light = $('body').hasClass('light');
  var bg = "transparent"; 
  var textcolor = {color: light ? '#848484' : '#a0a0a0'};
  var weak = light ? '#ccc' : '#3e3e3e';
  var strong = light ? '#909090' : '#707070';

  function elemToData(elem) {
    var data = new google.visualization.DataTable();
    $.each($(elem).data('columns'), function() {
      data.addColumn(this[0], this[1]);
    });
    data.addRows($(elem).data('rows'));

    return data;
  }

  $('div.elo_history').each(function() {
    var data = elemToData(this);
    var chart = new google.visualization.ComboChart(this);
    chart.draw(data, {
      series: [
    { color: "blue", type: "area", lineWidth: 2 },
      { color: "red", type: "line", lineWidth: 2 }
    ],
      width: 460,
      height: 340,
      axisTitlePosition: 'none',
      chartArea:{left:"10%",top:"2%",width:"90%",height:"96%"},
      titlePosition: 'none',
      hAxis: {textPosition: "none"},
      vAxis: {textStyle: textcolor, gridlines: lineColor},
      backgroundColor: bg
    });
  });

  $('div.win_stats').each(function() {
    var data = elemToData(this);
    var chart = new google.visualization.PieChart(this);
    chart.draw(data, {
      width: 312,
      height: 200,
      titlePosition: 'none',
      legend: {textStyle: textcolor},
      chartArea:{left:"0%",width:"100%",height:"100%"},
      is3D: true,
      backgroundColor: bg
    });
  });

  $('div.elo_distribution').each(function() {
    var data = elemToData(this);
    var chart = new google.visualization.ScatterChart(this);
    chart.draw(data, {
      width: 747,
      height: 500,
      //axisTitlePosition: 'none',
      chartArea:{left:"5%",top:"3%",width:"78%",height:"92%"},
      title: $(this).attr('title'),
      titlePosition: 'in',
      pointSize: 3,
      backgroundColor: bg
    });
  });

  $('div.end_distribution').each(function() {
    var data = elemToData(this);
    var chart = new google.visualization.PieChart(this);
    chart.draw(data, {
      width: 747,
      height: 500,
      chartArea:{left:"0%",top:"5%",width:"100%",height:"95%"},
      is3D: true,
      title: $(this).attr('title'),
      backgroundColor: bg
    });
  });

  $('div.move-time').each(function() {
    var data = elemToData(this);
    var chart = new google.visualization.AreaChart(this);
    chart.draw(data, {
      width: 747,
      height: 400,
      title: $(this).data('title'),
      titleTextStyle: textcolor,
      chartArea:{left:"5%",top:"5%",width:"78%",height:"90%"},
      backgroundColor: bg,
      vAxis: {textStyle: textcolor, gridlines: lineColor},
      legend: {textStyle: textcolor}
    });
  });

  $('div.adv_chart').each(function() {
    var data = elemToData(this);
    var chart = new google.visualization.AreaChart(this);
    chart.draw(data, {
      width: 512,
      height: 150,
      title: $(this).data('title'),
      titleTextStyle: textcolor,
      titlePosition: "in",
      chartArea:{left:"0%",top:"0%",width:"100%",height:"100%"},
      backgroundColor: bg,
      vAxis: {
        maxValue: $(this).data('max'),
        minValue: -$(this).data('max'),
        baselineColor: bg,
        gridlines: {color: bg},
        minorGridlines: {color: bg},
        viewWindowMode: "maximized"
      },
      legend: {position: "none"},
      axisTitlesPosition: "none"
    });
    google.visualization.events.addListener(chart, 'select', function() {
      var sel = chart.getSelection()[0];
      GoToMove(sel.row + 1);
    });
    $(this).data("chart", chart);
  });
}

$(function() {
  $.getScript('https://www.google.com/jsapi?autoload={"modules":[{"name":"visualization","version":"1","packages":["corechart"],"callback":"drawCharts"}]}');
});
