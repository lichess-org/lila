import { Chart, ChartDataset } from 'chart.js';
import { currentTheme } from 'common/theme';
import { Player } from './interface';

export interface MovePoint {
  y: number;
  x: number;
}

let highchartsPromise: Promise<any> | undefined;

// Add a slight offset so the graph doesn't get cutoff when eval = mate.
export const chartYMax = 1 + 0.05;
export const chartYMin = -chartYMax;

const lightTheme = currentTheme() == 'light';
export const orangeAccent = '#d85000';
export const whiteFill = lightTheme ? 'white' : '#676665';
export const blackFill = lightTheme ? '#999999' : 'black';
export const fontColor = lightTheme ? '#2F2F2F' : '#A0A0A0';
export const gridcolor = '#404040';
export const hoverBorderColor = lightTheme ? gridcolor : 'white';
export const tooltipBgColor = lightTheme ? 'rgba(255, 255, 255, 0.75)' : 'rgba(0,0,0,0.7)';

export function fontFamily(size?: number, weight?: string) {
  return {
    family: "'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif",
    size: size ?? 12,
    weight: weight,
  };
}

export function maybeChart(el: HTMLCanvasElement): Chart | undefined {
  const ctx = el.getContext('2d');
  if (ctx) return Chart.getChart(ctx);
  return undefined;
}

export function toBlurArray(player: Player) {
  return player.blurs?.bits?.split('') ?? [];
}

export function plyLine(ply: number, mainline = true): ChartDataset<'line'> {
  return {
    xAxisID: 'x',
    type: 'line',
    label: 'ply',
    data: [
      { x: ply, y: chartYMin },
      { x: ply, y: chartYMax },
    ],
    borderColor: orangeAccent,
    pointRadius: 0,
    pointHoverRadius: 0,
    borderWidth: 1,
    animation: false,
    segment: !mainline ? { borderDash: [5] } : undefined,
    order: 0,
  };
}

export function selectPly(this: Chart, ply: number, onMainline: boolean) {
  ply = ply - 1;
  const index = this.data.datasets.findIndex(dataset => dataset.label == 'ply');
  const line = plyLine(ply, onMainline);
  this.data.datasets[index] = line;
  this.update();
}

export function christmasTree(chart: Chart, mainline: Tree.Node[], hoverColors: string[]) {
  $('div.advice-summary').on('mouseenter', 'div.symbol', function (this: HTMLElement) {
    const symbol = this.getAttribute('data-symbol');
    const playerColorBit = this.getAttribute('data-color') == 'white' ? 1 : 0;
    const acplDataset = chart.data.datasets[0];
    if (symbol == '??' || symbol == '?!' || symbol == '?') {
      acplDataset.hoverBackgroundColor = hoverColors;
      acplDataset.borderColor = hoverColors;
      const points = mainline
        .map((node, i) =>
          node.glyphs?.some(glyph => glyph.symbol == symbol) && (node.ply & 1) == playerColorBit
            ? { datasetIndex: 0, index: i - 1 }
            : { datasetIndex: 0, index: -1 },
        )
        .filter(i => i.index >= 0);
      chart.setActiveElements(points);
      chart.update();
    }
  });
  $('div.advice-summary').on('mouseleave', 'div.symbol', function (this: HTMLElement) {
    if (chart.getActiveElements().length) chart.setActiveElements([]);
    chart.data.datasets[0].hoverBackgroundColor = orangeAccent;
    chart.data.datasets[0].borderColor = orangeAccent;
    chart.update();
  });
}

export async function loadHighcharts(tpe: string) {
  if (highchartsPromise) return highchartsPromise;
  const file = tpe === 'highstock' ? 'highstock.js' : 'highcharts.js';
  highchartsPromise = lichess.loadIife('npm/highcharts-4.2.5/' + file, {
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
