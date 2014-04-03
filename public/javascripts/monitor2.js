$(function() {

  var $monitors = $('#monitors');
  var charts = [];
  var disabled = {
    enabled: false
  };
  var noText = {
    text: null
  };
  var maxPoints = 60;
  var colors = Highcharts.theme.colors;
  var width = 375,
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
      defaultSeriesType: 'line',
      animation: false,
      borderRadius: 0,
    },
    title: {
      floating: true,
      style: {
        font: '12px Open Sans, Lucida Grande, Lucida Sans Unicode, Verdana, Arial, Helvetica, sans-serif',
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
    title: 'Active Users'
  });

  add({
    id: 'lat',
    title: 'Global Latency',
    type: 'area',
    format: '{value} ms'
  });

  add({
    id: 'rps',
    title: "HTTP Requests",
    maxVal: 200,
    threshold: 0.9
  });

  add({
    id: 'cpu',
    title: "JVM CPU",
    maxVal: 100,
    threshold: 0.3,
    format: "{value}%"
  });

  add({
    id: 'mps',
    title: "Chess Moves",
    maxVal: 100
  });

  add({
    id: 'load',
    title: "Load Average",
    maxVal: 1,
    threshold: 0.5
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

  add({
    id: 'memory',
    title: "JVM Memory",
    maxVal: 8192,
    format: "{value} MB"
  });

  add({
    id: 'thread',
    title: "JVM Threads",
    maxVal: 1000,
    threshold: 0.8
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
          var val = d[0];
          if (id == 'ai') {
            var loads = _.map(val.split(','), parseLoad);
            _.each(loads, updateAi);
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
