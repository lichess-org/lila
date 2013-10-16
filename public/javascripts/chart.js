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

  $('div.elo_distribution').each(function() {
    var data = elemToData(this);
    var chart = new google.visualization.ScatterChart(this);
    chart.draw(data, {
      width: 747,
      height: 500,
      //axisTitlePosition: 'none',
      chartArea: {
        left: "5%",
        top: "3%",
        width: "78%",
        height: "92%"
      },
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
      chartArea: {
        left: "0%",
        top: "5%",
        width: "100%",
        height: "95%"
      },
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

  $('div.adv_chart').each(function() {
    var data = elemToData(this);
    var chart = new google.visualization.AreaChart(this);
    chart.draw(data, {
      width: 512,
      height: 170,
      title: $(this).data('title'),
      titleTextStyle: textcolor,
      titlePosition: "in",
      chartArea: {
        left: "0%",
        top: "2%",
        width: "100%",
        height: "96%"
      },
      backgroundColor: bg,
      vAxis: {
        maxValue: $(this).data('max'),
        minValue: -$(this).data('max'),
        baselineColor: strong,
        gridlines: {
          color: bg
        },
        minorGridlines: {
          color: bg
        },
        viewWindowMode: "maximized"
      },
      legend: {
        position: "none"
      },
      axisTitlesPosition: "none"
    });
    google.visualization.events.addListener(chart, 'select', function() {
      try {
        var sel = chart.getSelection()[0];
        GoToMove(sel.row + 1);
      } catch (e) { }
    });
    $(this).data("chart", chart);
  });
}

$(function() {
  $.getScript('https://www.google.com/jsapi?autoload={"modules":[{"name":"visualization","version":"1","packages":["corechart"],"callback":"drawCharts"}]}');
});
