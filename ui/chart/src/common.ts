import { currentTheme } from '../../common/src/theme';
import Plotly from 'plotly.js-dist-min';
import { Division } from './interface';

function getFont() {
  return {
    family: "'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif",
    color: '#888',
  };
}

function divisionLine(division: string, ply: number) {
  const font = getFont();
  return {
    shape: {
      type: 'line',
      x0: ply - 1,
      x1: ply - 1,
      y0: -1,
      y1: 1,
      line: {
        color: '#888',
        width: 1,
      },
    } as Plotly.Shape,
    annotation: {
      font,
      xanchor: 'left',
      yanchor: 'top',
      x: ply - 1,
      y: 1,
      text: division,
      textangle: '90',
      showarrow: false,
    } as Partial<Plotly.Annotations>,
  };
}

async function divisionLines(div: Division | undefined, trans: Trans) {
  if (!div) return { shapes: [], annotations: [] };

  const lines = [];
  if (div.middle) {
    if (div.middle > 1) lines.push(divisionLine(trans('opening'), 0));
    lines.push(divisionLine(trans('middlegame'), div.middle - 1));
  }
  if (div.end) {
    if (div.end > 1 && !div.middle) lines.push(divisionLine(trans('middlegame'), 0));
    lines.push(divisionLine(trans('endgame'), div.end - 1));
  }

  const shapes = lines.map(({ shape }) => shape);
  const annotations = lines.map(({ annotation }) => annotation);

  return { shapes, annotations };
}

export async function createBase(
  el: HTMLElement,
  options: {
    min: number;
    max: number;
    indices?: [number, number];
    trans: Trans;
    division: Division | undefined;
  }
) {
  const { min, max, indices, trans, division } = options;

  (el as any).on(
    'plotly_click',
    (event: any) => event.points[0] && lichess.pubsub.emit('analysis.chart.click', event.points[0].x)
  );

  const applyTheme = async () => {
    const light = currentTheme() === 'light';
    const font = getFont();
    if (light) font.color = '#444';
    else font.color = '#DDD';

    if (light) Plotly.relayout(el, { hoverlabel: { bgcolor: '#FFF', bordercolor: '#444' }, font });
    else Plotly.relayout(el, { hoverlabel: { bgcolor: '#222', bordercolor: '#DDD' }, font });

    if (!indices) return;

    if (light) {
      Plotly.update(el, { 'line.color': '#48E' }, {}, indices[0]);
      Plotly.update(el, { 'line.color': '#000' }, {}, indices[1]);
    } else {
      Plotly.update(el, { 'line.color': '#AAA' }, {}, indices[0]);
      Plotly.update(el, { 'line.color': '#48E' }, {}, indices[1]);
    }
  };

  const lineInfo = await divisionLines(division, trans);
  Plotly.relayout(el, { annotations: lineInfo.annotations });

  const selectPly = (ply: number) => {
    Plotly.relayout(el, {
      shapes: [
        ...lineInfo.shapes,
        {
          type: 'line',
          x0: ply - 1,
          x1: ply - 1,
          y0: min,
          y1: max,
          line: {
            color: '#E84',
            width: 2,
          },
        },
      ],
    });
  };

  await applyTheme();
  lichess.pubsub.on('background-theme-changed', applyTheme);

  lichess.pubsub.on('ply', selectPly);
  lichess.pubsub.emit('ply.trigger');

  window.addEventListener('resize', () => Plotly.Plots.resize(el));

  return { selectPly };
}

let highchartsPromise: Promise<any> | undefined;

export async function loadHighcharts(tpe: string) {
  if (highchartsPromise) return highchartsPromise;
  const file = tpe === 'highstock' ? 'highstock.js' : 'highcharts.js';
  highchartsPromise = lichess.loadIife('vendor/highcharts-4.2.5/' + file, {
    noVersion: true,
  });
  await highchartsPromise;
  // Drop-in fix for Highcharts issue #8477 on older Highcharts versions. The
  // issue is fixed since Highcharts v6.1.1.
  const Highcharts = window.Highcharts;
  Highcharts.wrap(Highcharts.Axis.prototype, 'getPlotLinePath', function (this: any, proceed: any) {
    const path = proceed.apply(this, Array.prototype.slice.call(arguments, 1));
    if (path) path.flat = false;
    return path;
  });
  Highcharts.makeFont = function (size: number) {
    return (
      size + "px 'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif"
    );
  };
  Highcharts.theme = (() => {
    const light = currentTheme() === 'light';
    const text = {
      weak: light ? '#a0a0a0' : '#707070',
      strong: light ? '#707070' : '#a0a0a0',
    };
    const line = {
      weak: light ? '#ccc' : '#404040',
      strong: light ? '#a0a0a0' : '#606060',
      accent: '#d85000',
      white: '#ffffff',
      black: '#333333',
      grey: light ? '#999999' : '#777777',
    };
    const area = {
      white: light ? 'rgba(255,255,255,0.7)' : 'rgba(255,255,255,0.3)',
      black: light ? 'rgba(0,0,0,0.2)' : 'rgba(0,0,0,1)',
    };
    return {
      light: light,
      lichess: {
        text: text,
        line: line,
        area: area,
      },
      colors: [
        '#DDDF0D',
        '#7798BF',
        '#55BF3B',
        '#DF5353',
        '#aaeeee',
        '#ff0066',
        '#eeaaee',
        '#55BF3B',
        '#DF5353',
        '#7798BF',
        '#aaeeee',
      ],
      chart: {
        backgroundColor: null,
        borderWidth: 0,
        borderRadius: 0,
        plotBackgroundColor: null,
        plotShadow: false,
        plotBorderWidth: 0,
      },
      title: {
        style: {
          font: Highcharts.makeFont(13),
          color: text.strong,
        },
      },
      xAxis: {
        gridLineWidth: 0,
        gridLineColor: line.weak,
        lineColor: line.strong,
        tickColor: line.strong,
        labels: {
          style: {
            color: text.weak,
            fontWeight: 'bold',
          },
        },
        title: {
          style: {
            color: text.weak,
            font: Highcharts.makeFont(12),
          },
        },
        crosshair: {
          color: line.weak,
        },
      },
      yAxis: {
        alternateGridColor: null,
        minorTickInterval: null,
        gridLineColor: line.weak,
        minorGridLineColor: null,
        lineWidth: 0,
        tickWidth: 0,
        labels: {
          style: {
            color: text.weak,
            fontSize: '10px',
          },
        },
        title: {
          style: {
            color: text.weak,
            font: Highcharts.makeFont(12),
          },
        },
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
      lang: {
        thousandsSep: '',
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
      plotOptions: {
        series: {
          shadow: false,
          nullColor: '#444444',
        },
        line: {
          dataLabels: {
            color: text.strong,
          },
          marker: {
            lineColor: text.weak,
          },
        },
        spline: {
          marker: {
            lineColor: text.weak,
          },
        },
        scatter: {
          marker: {
            lineColor: text.weak,
          },
        },
        candlestick: {
          lineColor: text.strong,
        },
      },

      // highstock
      rangeSelector: light
        ? {}
        : {
            buttonTheme: {
              fill: '#505053',
              stroke: '#000000',
              style: {
                color: '#CCC',
              },
              states: {
                hover: {
                  fill: '#707073',
                  stroke: '#000000',
                  style: {
                    color: 'white',
                  },
                },
                select: {
                  fill: '#000003',
                  stroke: '#000000',
                  style: {
                    color: 'white',
                  },
                },
              },
            },
            inputBoxBorderColor: '#505053',
            inputStyle: {
              backgroundColor: '#333',
              color: 'silver',
            },
            labelStyle: {
              color: 'silver',
            },
          },

      navigator: light
        ? {}
        : {
            handles: {
              backgroundColor: '#666',
              borderColor: '#AAA',
            },
            outlineColor: '#CCC',
            maskFill: 'rgba(255,255,255,0.1)',
            series: {
              color: '#7798BF',
              lineColor: '#A6C7ED',
            },
            xAxis: {
              gridLineColor: '#505053',
            },
          },
    };
  })();
  Highcharts.setOptions(Highcharts.theme);
}
