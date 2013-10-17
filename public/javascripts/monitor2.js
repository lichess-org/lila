$(function() {

  var $monitors = $('#monitors');
  var charts = [];
  var disabled = {
    enabled: false
  };
  var noText = {
    text: null
  };
  var maxPoints = 12;
  var colors = Highcharts.theme.colors;
  var width = 355,
    height = 165;
  var counter = 0;
  var chartDefaults = {
    credits: disabled,
    legend: disabled,
    chart: {
      backgroundColor: {
        stops: [
          [0, 'rgb(46, 46, 46)'],
          [1, 'rgb(16, 16, 16)']
        ]
      },
      defaultSeriesType: 'spline',
      animation: false,
      borderRadius: 0,
    },
    title: {
      floating: true,
      style: {
        font: '12px Lucida Grande, Lucida Sans Unicode, Verdana, Arial, Helvetica, sans-serif',
        color: '#bababa',
      }
    },
    plotOptions: {
      series: {
        shadow: false
      }
    },
    xAxis: {
      type: 'datetime',
      title: noText,
      labels: disabled,
    },
    yAxis: {
      title: noText,
    }
  };

  function getMillis() {
    return (new Date()).getTime();
  }

  function add(info) {
    ++counter;
    var color = colors[counter % colors.length];
    $monitors.append($('<div>').attr('id', info.id).width(width).height(height));
    charts[info.id] = new Highcharts.Chart($.extend(true, {}, chartDefaults, {
      chart: {
        defaultSeriesType: info.type || 'spline',
        renderTo: info.id,
      },
      title: {
        text: info.title,
      },
      colors: [color],
      yAxis: {
        labels: {
          format: info.format || '{value}'
        }
      },
      series: [{
          name: info.title,
          data: []
        }
      ]
    }));
  }

  $monitors.append($('<div>').attr('id', 'allUsers').width(width * 2).height(height * 2));
  charts.allUsers = new Highcharts.Chart(
    $.extend(true, {}, chartDefaults, {
    chart: {
      renderTo: 'allUsers',
    },
    legend: {
      enabled: true,
      floating: true
    },
    title: {
      text: 'Users',
    },
    series: [{
        name: 'Total users',
        data: []
      }, {
        name: 'Playing users',
        data: []
      }, {
        name: 'Users on lobby',
        data: []
      }
    ]
  }));

  add({
    id: 'lat',
    title: 'Latency',
    type: 'areaspline',
    format: '{value} ms'
  });

  add({
    id: 'rps',
    title: "Requests",
    maxVal: 200,
    threshold: 0.9
  });

  add({
    id: 'memory',
    title: "Memory",
    maxVal: 8192,
    format: "{value} MB"
  });

  add({
    id: 'cpu',
    title: "CPU",
    maxVal: 100,
    threshold: 0.3,
    format: "{value}%"
  });

  add({
    id: 'mps',
    title: "Moves",
    maxVal: 100
  });

  add({
    id: 'thread',
    title: "Threads",
    maxVal: 1000,
    threshold: 0.8
  });

  add({
    id: 'load',
    title: "Load Average",
    maxVal: 1,
    threshold: 0.5
  });

  add({
    id: 'ai',
    title: "AI Load",
    maxVal: 100,
    threshold: 0.8
  });

  add({
    id: 'dbMemory',
    title: "MongoDB Memory",
    maxVal: 16384,
    threshold: 0.9,
    format: "{value} MB"
  });

  add({
    id: 'dbQps',
    title: "MongoDB Queries",
    maxVal: 2000,
    threshold: 0.8,
  });

  add({
    id: 'dbLock',
    title: "MongoDB Lock",
    maxVal: 10,
  });

  // add({
  //   id: 'dbConn',
  //   title: "MongoDB Connections",
  //   maxVal: 100,
  //   threshold: 0.8
  // });

  var lastCall = getMillis();

  var sri = Math.random().toString(36).substring(5);
  var wsUrl = "ws://socket." + document.domain + "/monitor/socket?sri=" + sri;
  var ws = window.MozWebSocket ? new MozWebSocket(wsUrl) : new WebSocket(wsUrl);
  ws.onmessage = function(e) {
    var m = JSON.parse(e.data);
    if (m.t == 'monitor') {
      var msg = m.d;
      var ds = msg.split(";");
      lastCall = getMillis();
      var allUsers = {};
      for (var i in ds) {
        var d = ds[i].split(":");
        if (d.length == 2) {
          var id = d[1];
          var val = parseFloat(d[0]);
          var chart, index = ['users', 'game', 'lobby'].indexOf(id);
          if (index != -1) {
            chart = charts.allUsers;
          } else {
            chart = charts[id];
            index = 0;
          }
          if (typeof chart != 'undefined') {
            var series = chart.series[index];
            var shift = series.data.length > maxPoints;
            var point = [lastCall, val];
            series.addPoint(point, true, shift);
          }
        }
      }
    }
  };

  function setStatus(s) {
    window.document.body.className = s;
  }

  setInterval(function() {
    if (getMillis() - lastCall > 3000) {
      setStatus("down");
    } else if (lastCall) {
      setStatus("up");
    }
  }, 1100);

});
