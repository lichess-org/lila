import { loadHighcharts } from './common';

export default async function (data: any) {
  await loadHighcharts('highchart');
  const trans = lichess.trans(data.i18n);
  const Highcharts = window.Highcharts;
  const disabled = { enabled: false };
  $('#rating_distribution_chart').each(function (this: HTMLElement) {
    const colors = Highcharts.getOptions().colors;
    const ratingAt = (i: number) => 600 + i * 25;
    const arraySum = (arr: number[]) => arr.reduce((a, b) => a + b, 0);
    const sum = arraySum(data.freq);
    const cumul = [];
    const buildRatingLine = (v: number, color: string, yCoord: number, label: string) => {
      const right = v > 1800;
      return v
        ? [
            {
              label: {
                text: label,
                verticalAlign: 'top',
                align: right ? 'right' : 'left',
                y: yCoord,
                x: right ? -5 : 5,
                style: {
                  color: color,
                },
                rotation: -0,
              },
              dashStyle: 'dash',
              color: color,
              width: 3,
              value: v,
            },
          ]
        : [];
    };
    for (let i = 0; i < data.freq.length; i++) cumul.push(Math.round((arraySum(data.freq.slice(0, i)) / sum) * 100));
    Highcharts.chart(this, {
      credits: disabled,
      legend: disabled,
      series: [
        {
          name: trans.noarg('players'),
          type: 'area',
          data: data.freq.map((nb: number, i: number) => [ratingAt(i), nb]),
          color: colors[1],
          fillColor: {
            linearGradient: {
              x1: 0,
              y1: 0,
              x2: 0,
              y2: 1.1,
            },
            stops: [
              [0, colors[1]],
              [1, Highcharts.Color(colors[1]).setOpacity(0).get('rgba')],
            ],
          },
          marker: {
            radius: 5,
          },
          lineWidth: 4,
        },
        {
          name: trans.noarg('cumulative'),
          type: 'line',
          yAxis: 1,
          data: cumul.map(function (p, i) {
            return [ratingAt(i), p];
          }),
          color: Highcharts.Color(colors[11]).setOpacity(0.8).get('rgba'),
          marker: {
            radius: 1,
          },
          shadow: true,
          tooltip: {
            valueSuffix: '%',
          },
        },
      ],
      chart: {
        zoomType: 'xy',
        alignTicks: false,
      },
      plotOptions: {},
      title: {
        text: null,
      },
      xAxis: {
        type: 'category',
        title: {
          text: trans.noarg('glicko2Rating'),
        },
        labels: {
          rotation: -45,
        },
        gridLineWidth: 1,
        gridZIndex: -1,
        tickInterval: 100,
        plotLines: buildRatingLine(data.myRating, colors[2], 13, trans.noarg('yourRating')).concat(
          buildRatingLine(data.otherRating, colors[6], 50, data.otherPlayer)
        ),
      },
      yAxis: [
        {
          // frequency
          title: {
            text: trans.noarg('players'),
          },
          gridZIndex: -1,
        },
        {
          // cumulative
          min: 0,
          max: 100,
          gridLineWidth: 0,
          title: {
            text: trans.noarg('cumulative'),
          },
          labels: {
            format: '{value}%',
          },
          opposite: true,
        },
      ],
    });
  });
}
