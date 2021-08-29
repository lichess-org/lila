import { h, VNode } from 'snabbdom';
import Ctrl from './ctrl';
import { Chart } from './interfaces';
import type * as Highcharts from 'highcharts';

function metricDataTypeFormat(dt: string) {
  if (dt === 'seconds') return '{point.y:.1f}';
  if (dt === 'average') return '{point.y:,.1f}';
  if (dt === 'percent') return '{point.y:.1f}%';
  return '{point.y:,.0f}';
}

function dimensionDataTypeFormat(dt: string) {
  if (dt === 'date') return '{value:%Y-%m-%d}';
  return '{value}';
}

function yAxisTypeFormat(dt: string) {
  if (dt === 'seconds') return '{value:.1f}';
  if (dt === 'average') return '{value:,.1f}';
  if (dt === 'percent') return '{value:.0f}%';
  return '{value:,.0f}';
}

const colors = {
  green: '#759900',
  red: '#dc322f',
  orange: '#d59120',
  blue: '#007599',
};
const resultColors = {
  Victory: colors.green,
  Draw: colors.blue,
  Defeat: colors.red,
};

interface Theme {
  light: boolean;
  text: {
    weak: string;
    strong: string;
  };
  line: {
    weak: string;
    strong: string;
    fat: string;
  };
  colors?: string[];
}

const theme = (function () {
  const light = $('body').hasClass('light');
  const t: Theme = {
    light: light,
    text: {
      weak: light ? '#808080' : '#9a9a9a',
      strong: light ? '#505050' : '#c0c0c0',
    },
    line: {
      weak: light ? '#ccc' : '#404040',
      strong: light ? '#a0a0a0' : '#606060',
      fat: '#d85000', // light ? '#a0a0a0' : '#707070'
    },
  };
  if (!light)
    t.colors = [
      '#2b908f',
      '#90ee7e',
      '#f45b5b',
      '#7798BF',
      '#aaeeee',
      '#ff0066',
      '#eeaaee',
      '#55BF3B',
      '#DF5353',
      '#7798BF',
      '#aaeeee',
    ];
  return t;
})();

function makeChart(el: HTMLElement, data: Chart) {
  const sizeSerie = {
    name: data.sizeSerie.name,
    data: data.sizeSerie.data,
    yAxis: 1,
    type: 'column',
    stack: 'size',
    animation: {
      duration: 300,
    },
    color: 'rgba(120,120,120,0.2)',
  };
  const valueSeries = data.series.map(function (s) {
    const c: Highcharts.ColumnChartSeriesOptions = {
      name: s.name,
      data: s.data,
      yAxis: 0,
      type: 'column',
      stack: s.stack,
      // animation: {
      //   duration: 300
      // },
      dataLabels: {
        enabled: true,
        format: s.stack ? '{point.percentage:.0f}%' : metricDataTypeFormat(s.dataType),
      },
      tooltip: {
        // headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
        pointFormat: (function () {
          return (
            '<span style="color:{point.color}">\u25CF</span> {series.name}: <b>' +
            metricDataTypeFormat(s.dataType) +
            '</b><br/>'
          );
        })(),
        shared: true,
      } as Highcharts.SeriesTooltipOptions,
    };
    if (data.valueYaxis.name === 'Game result') c.color = resultColors[s.name as 'Victory' | 'Draw' | 'Defeat'];
    return c;
  });
  const chartConf: Highcharts.Options = {
    chart: {
      type: 'column',
      alignTicks: data.valueYaxis.dataType !== 'percent',
      spacing: [20, 7, 20, 5],
      backgroundColor: undefined,
      borderWidth: 0,
      borderRadius: 0,
      plotBackgroundColor: undefined,
      plotShadow: false,
      plotBorderWidth: 0,
      style: {
        font: "12px 'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif",
      },
    },
    title: {
      text: undefined,
    },
    xAxis: {
      type: data.xAxis.dataType === 'date' ? 'datetime' : 'linear',
      categories: data.xAxis.categories.map(function (v) {
        return (data.xAxis.dataType === 'date' ? v * 1000 : v) as any;
      }),
      crosshair: true,
      labels: {
        format: dimensionDataTypeFormat(data.xAxis.dataType),
        style: {
          color: theme.text.weak,
          fontSize: '9',
        },
      },
      title: {
        style: {
          color: theme.text.weak,
          fontSize: '9',
        },
      },
      gridLineColor: theme.line.weak,
      lineColor: theme.line.strong,
      tickColor: theme.line.strong,
    },
    yAxis: [data.valueYaxis, data.sizeYaxis].map(function (a, i) {
      const isPercent = data.valueYaxis.dataType === 'percent';
      const isSize = i % 2 === 1;
      const c: Highcharts.AxisOptions = {
        opposite: isSize,
        min: !isSize && isPercent ? 0 : undefined,
        max: !isSize && isPercent ? 100 : undefined,
        labels: {
          format: yAxisTypeFormat(a.dataType),
          style: {
            color: theme.text.weak,
            fontSize: '9',
          },
        },
        title: {
          text: i === 1 ? a.name : '',
          style: {
            color: theme.text.weak,
            fontSize: '9',
          },
        },
        gridLineColor: theme.line.weak,
      };
      if (isSize && isPercent) {
        c.minorGridLineWidth = 0;
        c.gridLineWidth = 0;
        c.alternateGridColor = undefined;
      }
      return c;
    }),
    plotOptions: {
      column: {
        animation: {
          duration: 300,
        },
        stacking: 'normal',
        dataLabels: {
          color: theme.text.strong,
        },
        marker: {
          lineColor: theme.text.weak,
        },
        borderColor: theme.line.strong,
      },
    },
    series: valueSeries.concat(sizeSerie),
    credits: {
      enabled: false,
    },
    labels: {
      style: {
        color: theme.text.strong,
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
        stops: theme.light
          ? [
              [0, 'rgba(200, 200, 200, .8)'],
              [1, 'rgba(250, 250, 250, .8)'],
            ]
          : [
              [0, 'rgba(56, 56, 56, .8)'],
              [1, 'rgba(16, 16, 16, .8)'],
            ],
      },
      style: {
        fontWeight: 'bold',
        color: theme.text.strong,
      },
    },
    legend: {
      enabled: true,
      itemStyle: {
        color: theme.text.weak,
      },
      itemHiddenStyle: {
        color: theme.text.weak,
      },
    },
  };
  if (theme.colors) chartConf.colors = theme.colors;
  window.Highcharts.chart(el, chartConf);
}

function empty(txt: string) {
  return h('div.chart.empty', [
    h('i', {
      attrs: { 'data-icon': '' },
    }),
    txt,
  ]);
}

function chartHook(vnode: VNode, ctrl: Ctrl) {
  const el = vnode.elm as HTMLElement;
  if (ctrl.vm.loading || !ctrl.vm.answer) {
    $(el).html(lichess.spinnerHtml);
  } else {
    makeChart(el, ctrl.vm.answer);
  }
}

export default function (ctrl: Ctrl) {
  if (!ctrl.validCombinationCurrent()) return empty('Invalid dimension/metric combination');
  if (!ctrl.vm.answer?.series.length) return empty('No data. Try widening or clearing the filters.');
  return h('div.chart', {
    hook: {
      insert: vnode => chartHook(vnode, ctrl),
      update: (_oldVnode, newVnode) => chartHook(newVnode, ctrl),
    },
  });
}
