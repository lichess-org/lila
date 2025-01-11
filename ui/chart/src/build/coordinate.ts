import type { Chart, ChartConfiguration } from 'chart.js';
import { blueLineColor, fontColor, fontFamily, tooltipBgColor } from '../common';

function main(canvas: HTMLCanvasElement, data: number[], color: Color): Chart {
  const isSente = color === 'sente';
  const config: ChartConfiguration<'line'> = {
    type: 'line',
    data: {
      labels: data.map(() => ''),
      datasets: [
        {
          data: data,
          borderColor: blueLineColor,
          borderWidth: 1,
          backgroundColor: isSente ? 'white' : 'black',
          fill: true,
          pointHitRadius: 200,
          pointHoverBorderColor: '#3893e8',
          pointRadius: 0,
          pointHoverRadius: 5,
          spanGaps: true,
        },
      ],
    },
    options: {
      layout: {
        padding: 10,
      },
      plugins: {
        legend: {
          display: false,
        },
        tooltip: {
          borderColor: fontColor,
          borderWidth: 1,
          backgroundColor: tooltipBgColor,
          caretPadding: 15,
          titleColor: fontColor,
          titleFont: fontFamily(13),
          displayColors: false,
        },
        datalabels: {
          display: false,
        },
      },
      scales: {
        x: {
          display: false,
        },
        y: {
          display: false,
        },
      },
    },
  };
  return new window.Chart(canvas, config);
}

window.lishogi.registerModule(__bundlename__, main);
