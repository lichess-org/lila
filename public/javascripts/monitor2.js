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
  var colors = ['#2f7ed8', '#0d233a', '#8bbc21', '#910000', '#1aadce', '#492970', '#f28f43', '#77a1e5', '#c42525', '#a6c96a'];
  var width = 350,
    height = 160;
  var counter = 0;
  var chartDefaults = {
    credits: disabled,
    legend: disabled,
    chart: {
      defaultSeriesType: 'spline',
      animation: false,
      borderRadius: 2
    },
    title: {
      floating: true,
      style: {
        color: '#bababa',
        fontSize: '12px'
      }
    },
    xAxis: {
      type: 'datetime',
      title: noText,
      labels: disabled,
    },
    yAxis: {
      title: noText,
      min: 0,
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

  $monitors.append($('<div>').attr('id', 'allUsers').width(width * 2 + 5).height(height * 2 + 5));
  charts['allUsers'] = new Highcharts.Chart(
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
    type: 'column',
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
    type: 'line'
  });

  add({
    id: 'dbQps',
    title: "MongoDB Queries",
    maxVal: 2000,
    threshold: 0.8,
    type: 'line'
  });

  add({
    id: 'dbLock',
    title: "MongoDB Lock",
    maxVal: 10,
    type: 'line'
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
          var index = ['users', 'game', 'lobby'].indexOf(id)
          if (index != -1) {
            var chart = charts['allUsers']
          } else {
            var chart = charts[id];
            index = 0;
          }
          if (typeof chart != 'undefined') {
            var series = chart.series[index];
            var shift = series.data.length > maxPoints;
            var point = [lastCall, val];
            series.addPoint(point, true, shift);
          } else {
            console.debug(d);
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
