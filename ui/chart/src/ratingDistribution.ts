import { Point } from 'chart.js/dist/core/core.controller';
import { animation, fontFamily, gridColor, hoverBorderColor, resizePolyfill } from './common';
import { DistributionData } from './interface';
import {
  Chart,
  ChartConfiguration,
  ChartData,
  ChartDataset,
  Filler,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import ChartDataLabels from 'chartjs-plugin-datalabels';

resizePolyfill();
Chart.register(LineController, LinearScale, PointElement, LineElement, Tooltip, Filler, ChartDataLabels);

export async function initModule(data: DistributionData) {
  $('#rating_distribution_chart').each(function (this: HTMLCanvasElement) {
    const ratingAt = (i: number) => 400 + i * 25;
    const arraySum = (arr: number[]) => arr.reduce((a, b) => a + b, 0);
    const sum = arraySum(data.freq);
    const cumul: number[] = [];
    const ratings: number[] = [];
    for (let i = 0; i < data.freq.length; i++) {
      ratings.push(ratingAt(i));
      cumul.push(arraySum(data.freq.slice(0, i)) / sum);
    }
    const gradient = this.getContext('2d')?.createLinearGradient(0, 0, 0, 400);
    gradient?.addColorStop(0, 'rgba(119, 152, 191, 1)');
    gradient?.addColorStop(1, 'rgba(119, 152, 191, 0.3)');
    const seriesCommonData = (color: string): Partial<ChartDataset<'line'>> => ({
      pointHoverRadius: 6,
      pointHoverBorderWidth: 2,
      pointHoverBorderColor: hoverBorderColor,
      borderColor: color,
      pointBackgroundColor: color,
    });
    const maxRating = Math.max(...ratings);

    const datasets: ChartDataset<'line'>[] = [
      {
        ...seriesCommonData('#dddf0d'),
        data: cumul,
        yAxisID: 'y2',
        label: data.i18n.cumulative,
        pointRadius: 0,
        datalabels: { display: false },
        pointHitRadius: 200,
      },
      {
        ...seriesCommonData('#7798bf'),
        data: data.freq,
        backgroundColor: gradient,
        yAxisID: 'y',
        fill: true,
        label: data.i18n.players,
        pointRadius: 4,
        datalabels: { display: false },
        pointHitRadius: 200,
      },
    ];
    const pushLine = (color: string, rating: number, label: string) =>
      datasets.push({
        ...seriesCommonData(color),
        yAxisID: 'y2',
        data: [
          { x: rating, y: 0 },
          { x: rating, y: Math.max(...cumul) },
        ],
        segment: {
          borderDash: [10],
        },
        label: label,
        pointRadius: 4,
        datalabels: {
          align: 'top',
          offset: 0,
          display: 'auto',
          formatter: (value: Point) => (value.y == 0 ? '' : label),
          color: color,
        },
      });
    if (data.myRating && data.myRating <= maxRating) pushLine('#55bf3b', data.myRating, data.i18n.yourRating);
    if (data.otherRating && data.otherPlayer && data.otherRating <= maxRating)
      pushLine('#eeaaee', data.otherRating, data.otherPlayer);
    const chartData: ChartData<'line'> = {
      labels: ratings,
      datasets: datasets,
    };

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: chartData,
      options: {
        scales: {
          x: {
            type: 'linear',
            min: Math.min(...ratings),
            max: maxRating,
            grid: {
              color: gridColor,
            },
            ticks: {
              stepSize: 100,
              format: {
                useGrouping: false,
              },
            },
            title: {
              display: true,
              text: data.i18n.glicko2Rating,
            },
          },
          y: {
            grid: {
              color: gridColor,
              tickLength: 0,
            },
            ticks: {
              padding: 10,
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
              format: {
                style: 'percent',
                maximumFractionDigits: 1,
              },
            },
            title: {
              display: true,
              text: data.i18n.cumulative,
            },
          },
        },
        animations: animation(1000 / ratings.length),
        locale: document.documentElement.lang,
        maintainAspectRatio: false,
        responsive: true,
        plugins: {
          tooltip: {
            titleFont: fontFamily(),
            bodyFont: fontFamily(),
            caretPadding: 8,
            callbacks: {
              label: item => (item.datasetIndex > 1 ? item.dataset.label : undefined),
            },
          },
        },
      },
    };
    new Chart(this, config);
  });
}
