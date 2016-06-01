var m = require('mithril');

var once = false;

module.exports = function(ctrl) {
  var noText = {text: null};
  var disabled = {enabled: false};
  var noAnimation = {animation: disabled};
  return m('div', {
    config: function (el, isUpdate) {
      if (once) return;
      if (!window.Highcharts) return;
      once = true;
      withHighcharts(function () {
      $(el).highcharts({
        chart: {
          type: 'area',
          spacing: [2, 0, 2, 0]
        },
        series: [{
          name: 'Advantage',
          data: [
            { name: "hello", x: 0, y: 32 },
            { name: "hello", x: 1, y: 22 },
            { name: "hello", x: 2, y: -32 },
            { name: "hello", x: 3, y: 50 },
            { name: "hello", x: 4, y: -200 }
          ]
        }],
        plotOptions: {
          area: {
            fillColor: Highcharts.theme.lichess.area.white,
            negativeFillColor: Highcharts.theme.lichess.area.black,
            fillOpacity: 1,
            threshold: 0,
            lineWidth: 2,
            color: Highcharts.theme.lichess.line.fat,
            allowPointSelect: true,
            column: noAnimation,
            cursor: 'pointer',
            events: {
              click: function(event) {
                if (event.point) {
                  event.point.select();
                  ctrl.jumpToIndex(event.point.x);
                }
              }
            },
            marker: {
              radius: 1,
              states: {
                hover: {
                  radius: 3,
                  lineColor: '#b57600',
                  fillColor: '#ffffff'
                },
                select: {
                  radius: 4,
                  lineColor: '#b57600',
                  fillColor: '#ffffff'
                }
              }
            }
          }
        },
        title: noText,
        xAxis: {
          title: noText,
          labels: disabled,
          lineWidth: 0,
          tickWidth: 0,
        },
        yAxis: {
          title: noText,
          labels: disabled,
          lineWidth: 1,
          gridLineWidth: 0,
          plotLines: [{
            color: Highcharts.theme.lichess.text.weak,
            width: 1,
            value: 0
          }]
        },
        credits: disabled,
        legend: disabled
      });
    } });
  });
};
