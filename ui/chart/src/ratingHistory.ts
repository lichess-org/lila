import { currentTheme } from '../../common/src/theme';
import Plotly from 'plotly.js-dist-min';

interface Opts {
  data: any;
  singlePerfName: string;
  perfIndex: number;
}

export async function initModule({ data, singlePerfName }: Opts) {
  const oneDay = 86400000;
  function smoothDates(points: any[]) {
    if (!points.length) return [];

    const data = points.map(r => [Date.UTC(r[0], r[1], r[2]), r[3]]);

    const begin = data[0][0];
    const end = data[data.length - 1][0];
    const reversed = data.slice().reverse();

    const x: Date[] = [];
    const y: number[] = [];
    for (let date = begin; date <= end; date += oneDay) {
      const match = reversed.find((x: number[]) => x[0] <= date)!;
      x.push(new Date(date));
      y.push(match[1]);
    }
    return { x, y };
  }
  const $el = $('div.rating-history');
  const singlePerfIndex = data.findIndex((x: any) => x.name === singlePerfName);
  if (singlePerfName && data[singlePerfIndex].points.length === 0) {
    $el.hide();
    return;
  }

  $el.each(function (this: HTMLElement) {
    let usedData: any[] = data;
    let styles: Partial<Plotly.ScatterLine>[] = [
      // order of perfs from RatingChartApi.scala
      { color: '#56B4E9', dash: 'solid' }, // Bullet
      { color: '#0072B2', dash: 'solid' }, // Blitz
      { color: '#009E73', dash: 'solid' }, // Rapid
      { color: '#459F3B', dash: 'solid' }, // Classical
      { color: '#F0E442', dash: 'dash' }, // Correspondence
      { color: '#E69F00', dash: 'dash' }, // Chess960
      { color: '#D55E00', dash: 'dash' }, // KotH
      { color: '#CC79A7', dash: 'dot' }, // 3+
      { color: '#DF5353', dash: 'dot' }, // Anti
      { color: '#66558C', dash: 'dot' }, // Atomic
      { color: '#99E699', dash: 'dashdot' }, // Horde
      { color: '#FFAEAA', dash: 'dot' }, // Racing Kings
      { color: '#56B4E9', dash: 'dashdot' }, // Crazyhouse
      { color: '#0072B2', dash: 'dashdot' }, // Puzzle
      { color: '#009E73', dash: 'dashdot' }, // Ultrabullet
    ];

    if (singlePerfName) {
      usedData = [usedData[singlePerfIndex]];
      styles = [styles[singlePerfIndex]];
    }

    window.addEventListener('resize', () => Plotly.Plots.resize(this));

    const applyTheme = () => {
      const light = currentTheme() === 'light';
      const bgcolor = light ? '#FFFFFF' : '#262421';
      const font = {
        family: "'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif",
        color: light ? '#333333' : '#EEEEEE',
      };
      Plotly.relayout(this, {
        paper_bgcolor: bgcolor,
        plot_bgcolor: bgcolor,
        font,
      });
    };

    this.textContent = '';
    Plotly.newPlot(
      this,
      usedData.map((data, i) => ({ name: data.name, ...smoothDates(data.points), line: styles[i] })),
      {
        showlegend: false,
        hovermode: 'x unified',
        margin: { t: 0, b: 16, l: 0, r: 0 },
        yaxis: { fixedrange: true },
        dragmode: 'pan',
      },
      {
        displayModeBar: false,
        showTips: false,
        scrollZoom: true,
      }
    );

    applyTheme();
    lichess.pubsub.on('background-theme-changed', applyTheme);
  });
}
