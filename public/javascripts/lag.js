lichess.load.then(() => {
  Highcharts.makeFont = function (size) {
    return size + "px 'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif";
  };
  Highcharts.theme = (function () {
    var light = $('body').hasClass('light');
    var text = {
      weak: light ? '#a0a0a0' : '#707070',
      strong: light ? '#707070' : '#a0a0a0',
    };
    var line = {
      weak: light ? '#ccc' : '#404040',
      strong: light ? '#a0a0a0' : '#606060',
      fat: '#d85000', // light ? '#a0a0a0' : '#707070'
    };
    return {
      light: light,
      lichess: {
        text: text,
        line: line,
      },
      chart: {
        backgroundColor: null,
        borderWidth: 0,
        borderRadius: 0,
        plotBackgroundColor: null,
        plotShadow: false,
        plotBorderWidth: 0,
      },
      title: {
        text: null,
      },
      legend: {
        itemStyle: {
          color: text.strong,
        },
        itemHiddenStyle: {
          color: text.weak,
        },
      },
      labels: {
        style: {
          color: text.strong,
        },
      },
      plotOptions: {
        series: {
          dataLabels: {
            align: 'left',
          },
          wrap: false,
        },
      },
      tooltip: {
        backgroundColor: {
          linearGradient: {
            x1: 0,
            y1: 0,
            x2: 0,
            y2: 1,
          },
          stops: light
            ? [
                [0, 'rgba(200, 200, 200, .8)'],
                [1, 'rgba(250, 250, 250, .8)'],
              ]
            : [
                [0, 'rgba(56, 56, 56, .8)'],
                [1, 'rgba(16, 16, 16, .8)'],
              ],
        },
        borderWidth: 0,
        style: {
          fontWeight: 'bold',
          color: text.strong,
        },
      },
    };
  })();
  Highcharts.setOptions(Highcharts.theme);

  var buildChart = function (opt) {
    return {
      chart: {
        type: 'gauge',
        plotBackgroundColor: null,
        plotBackgroundImage: null,
        plotBorderWidth: 0,
        plotShadow: false,
        backgroundColor: null,
        animation: {
          duration: 1000,
          // easing: 'easeOutBounce'
        },
      },
      credits: false,

      title: {
        text: false,
      },

      pane: {
        startAngle: -150,
        endAngle: 150,
        background: [
          {
            backgroundColor: {
              linearGradient: {
                x1: 0,
                y1: 0,
                x2: 0,
                y2: 1,
              },
              stops: [
                [0, '#FFF'],
                [1, '#333'],
              ],
            },
            borderWidth: 0,
            outerRadius: '109%',
          },
          {
            backgroundColor: {
              linearGradient: {
                x1: 0,
                y1: 0,
                x2: 0,
                y2: 1,
              },
              stops: [
                [0, '#333'],
                [1, '#FFF'],
              ],
            },
            borderWidth: 1,
            outerRadius: '107%',
          },
          {
            // default background
          },
          {
            backgroundColor: '#DDD',
            borderWidth: 0,
            outerRadius: '105%',
            innerRadius: '103%',
          },
        ],
      },

      // the value axis
      yAxis: {
        min: 0,
        max: 750,

        minorTickInterval: 'auto',
        minorTickWidth: 1,
        minorTickLength: 10,
        minorTickPosition: 'inside',
        minorTickColor: '#666',

        tickPixelInterval: 30,
        tickWidth: 2,
        tickPosition: 'inside',
        tickLength: 10,
        tickColor: '#666',
        labels: {
          step: 2,
          rotation: 'auto',
        },
        title: {
          text: opt.title + '<br>milliseconds',
        },
        plotBands: [
          {
            from: 0,
            to: 500,
            color: '#55BF3B', // green
          },
          {
            from: 500,
            to: 650,
            color: '#DDDF0D', // yellow
          },
          {
            from: 650,
            to: 750,
            color: '#DF5353', // red
          },
        ],
      },

      series: [
        {
          name: 'Latency',
          data: [0],
          tooltip: {
            valueSuffix: ' milliseconds',
          },
        },
      ],
    };
  };

  var charts = {};

  Highcharts.chart(
    document.querySelector('.server .meter'),
    buildChart({
      title: 'SERVER',
    }),
    function (c) {
      charts.server = c;
    }
  );
  Highcharts.chart(
    document.querySelector('.network .meter'),
    buildChart({
      title: 'PING',
    }),
    function (c) {
      charts.network = c;
    }
  );
  var values = {
    server: -1,
    network: -1,
  };

  var updateAnswer = function () {
    if (values.server === -1 || values.network === -1) return;
    var c;
    if (values.server <= 100 && values.network <= 500) c = 'nope-nope';
    else if (values.server <= 100) c = 'nope-yep';
    else c = 'yep';
    $('.lag .answer span')
      .addClass('none')
      .parent()
      .find('.' + c)
      .removeClass('none');
  };

  lichess.StrongSocket.firstConnect.then(() => lichess.socket.send('moveLat', true));

  lichess.pubsub.on('socket.in.mlat', d => {
    const v = parseInt(d);
    charts.server.series[0].points[0].update(v);
    values.server = v;
    updateAnswer();
  });

  setInterval(function () {
    const v = Math.round(lichess.socket.averageLag);
    charts.network.series[0].points[0].update(v);
    values.network = v;
    updateAnswer();
  }, 1000);
});
