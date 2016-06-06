$(function() {

  var $monitors = $('#monitors');
  var charts = [];
  var disabled = {
    enabled: false
  };
  var noText = {
    text: null
  };
  var colors = Highcharts.theme.colors;
  var height = 165;
  var width = document.body.clientWidth / 2 - 25;
  var maxPoints = width / 4;
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
      defaultSeriesType: 'line',
      animation: false,
      borderRadius: 0,
    },
    title: {
      floating: true,
      style: {
        font: '12px Noto Sans, Lucida Grande, Lucida Sans Unicode, Verdana, Arial, Helvetica, sans-serif',
        color: '#bababa',
      }
    },
    plotOptions: {
      series: {
        shadow: false,
        marker: {
          lineColor: null,
          fillColor: 'none'
        }
      }
    },
    xAxis: {
      type: 'datetime',
      title: noText,
      labels: disabled,
    },
    yAxis: {
      title: noText,
      opposite: true
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
        defaultSeriesType: info.type || 'line',
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

  add({
    id: 'users',
    title: 'Open Websockets'
  });

  add({
    id: 'lat',
    title: 'Report generation time',
    type: 'area',
    format: '{value} ms'
  });

  add({
    id: 'rps',
    title: "HTTP Requests /s",
  });

  add({
    id: 'cpu',
    title: "JVM CPU usage",
    format: "{value}%"
  });

  add({
    id: 'mps',
    title: "Chess Moves /s"
  });

  add({
    id: 'mlat',
    title: "Time to process a move",
    format: '{value} ms'
  });

  add({
    id: 'load',
    title: "Load Average"
  });

  add({
    id: 'dbMemory',
    title: "MongoDB Memory usage",
    format: "{value} MB"
  });

  add({
    id: 'dbQps',
    title: "MongoDB Queries /s",
  });

  add({
    id: 'memory',
    title: "JVM Memory usage",
    format: "{value} MB"
  });

  add({
    id: 'thread',
    title: "JVM Threads"
  });

  // add({
  //   id: 'dbConn',
  //   title: "MongoDB Connections",
  // });

  var lastCall = getMillis();

  var sri = Math.random().toString(36).substring(5);
  var wsUrl = "wss://socket." + document.domain + "/monitor/socket?sri=" + sri;
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
          var val = d[0];
          if (id == 'ai') {
            var loads = val.split(',').map(parseLoad);
            loads.forEach(updateAi);
          } else update(id, 0, parseFloat(val));
        }
      }
    }
  };

  function update(id, serie, val) {
    var chart = charts[id];
    if (!isNaN(val) && typeof chart != 'undefined') {
      var series = chart.series[serie];
      var shift = series.data.length > maxPoints;
      var point = [lastCall, val];
      series.addPoint(point, true, shift);
    }
  }

  function updateAi(load, i) {
    update('ai', i, load);
  }

  function parseLoad(load) {
    return parseInt(load, 10);
  }

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
