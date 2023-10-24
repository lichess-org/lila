import { DistributionData } from './interface';
import * as chart from 'chart.js';

chart.Chart.register(
  chart.LineController,
  chart.LinearScale,
  chart.PointElement,
  chart.LineElement,
  chart.Tooltip,
  chart.Filler,
);

export async function initModule(data: DistributionData) {
  $('#rating_distribution_chart').each(function (this: HTMLCanvasElement) {
    const ratingAt = (i: number) => 400 + i * 25;
    const arraySum = (arr: number[]) => arr.reduce((a, b) => a + b, 0);
    const sum = arraySum(data.freq);
    const cumul: [number, number][] = [];
    for (let i = 0; i < data.freq.length; i++)
      cumul.push([ratingAt(i), Math.round((arraySum(data.freq.slice(0, i)) / sum) * 100)]);
    const gradient = this.getContext('2d')?.createLinearGradient(0, 0, 0, 400);
    gradient?.addColorStop(0, 'rgba(119, 152, 191, 1)');
    gradient?.addColorStop(1, 'rgba(119, 152, 191, 0.2)');
    const seriesCommonData = (color: string) => ({
      pointHoverRadius: 5,
      pointHoverBorderColor: 'white',
      borderColor: color,
      pointBackgroundColor: color,
      pointHitRadius: 200,
    });
    const gridcolor = '#404040';

    const datasets: chart.Chart['data']['datasets'] = [
      {
        ...seriesCommonData('#dddf0d'),
        data: cumul.map(x => x[1]),
        yAxisID: 'y2',
        label: data.i18n.cumulative,
      },
      {
        ...seriesCommonData('#7798bf'),
        data: data.freq.map((nb: number) => nb),
        backgroundColor: gradient,
        yAxisID: 'y',
        fill: true,
        label: data.i18n.players,
      },
    ];
    const pushLine = (color: string, rating: number, label: string) =>
      datasets.push({
        ...seriesCommonData(color),
        yAxisID: 'y2',
        data: [
          [rating, 0],
          [rating, 100],
        ],
        segment: {
          borderDash: [10],
        },
        label: label,
        pointRadius: 4,
      });
    if (data.myRating) pushLine('#55bf3b', data.myRating, data.i18n.yourRating);
    if (data.otherRating && data.otherPlayer) pushLine('#eeaaee', data.otherRating, data.otherPlayer);
    const chartData: chart.Chart['data'] = {
      labels: cumul.map(x => x[0]),
      datasets: datasets,
    };

    const config: chart.Chart['config'] = {
      type: 'line',
      data: chartData,
      options: {
        scales: {
          x: {
            type: 'linear',
            grid: {
              color: gridcolor,
            },
            ticks: {
              maxTicksLimit: 25,
              callback: val => `${val}`, // remove thousands separator
            },
            title: {
              display: true,
              text: data.i18n.glicko2Rating,
            },
          },
          y: {
            grid: {
              color: gridcolor,
              tickLength: 0,
            },
            ticks: {
              padding: 10,
              precision: 0,
            },
            title: {
              display: true,
              text: data.i18n.players,
            },
          },
          y2: {
            position: 'right',
            grid: {
              display: false,
            },
            ticks: {
              callback: val => `${val}%`,
            },
            title: {
              display: true,
              text: data.i18n.cumulative,
            },
          },
        },
        maintainAspectRatio: false,
        responsive: true,
        plugins: {
          tooltip: {
            rtl: document.dir == 'rtl',
            callbacks: {
              label: item => {
                switch (item.datasetIndex) {
                  case 0:
                    return `${data.i18n.cumulative}: ${item.formattedValue}%`;
                  case 1:
                    return `${data.i18n.players}: ${item.formattedValue}`;
                  //Annotation tooltip formatting hacks:
                  case 2:
                    return data.i18n.yourRating;
                  case 3:
                    return data.otherPlayer!;
                  default:
                    return item.formattedValue;
                }
              },
              title: items => items[0].label.replace(/,|\./, ''),
            },
          },
        },
      },
    };
    new chart.Chart(this, config);
  });
}
