import { loadHighcharts } from './common';

interface Opts {
  data: any;
  singlePerfName: string;
  perfIndex: number;
}

export async function initModule({ data, singlePerfName, perfIndex }: Opts) {
  const oneDay = 86400000;
  function smoothDates(data: any[]) {
    if (!data.length) return [];

    const begin = data[0][0];
    const end = data[data.length - 1][0];
    const reversed = data.slice().reverse();
    const allDates: any[] = [];
    for (let i = begin - oneDay; i <= end; i += oneDay) allDates.push(i);
    const result = [];
    for (let j = 1; j < allDates.length; j++) {
      const match = reversed.find((x: number[]) => x[0] <= allDates[j]);
      result.push([allDates[j], match[1]]);
    }
    return result;
  }
  const $el = $('div.rating-history');
  const singlePerfIndex = data.findIndex((x: any) => x.name === singlePerfName);
  if (singlePerfName && !data[singlePerfIndex]?.points.length) {
    $el.hide();
    return;
  }
  const indexFilter = (_: any, i: number) => !singlePerfName || i === singlePerfIndex;
  await loadHighcharts('highstock');
  // support: Fx when user bio overflows
  const disabled = { enabled: false };
  const noText = { text: null };
  $el.each(function (this: HTMLElement) {
    const dashStyles = [
      // order of perfs from RatingChartApi.scala
      'Solid', // Bullet
      'Solid', // Blitz
      'Solid', // Rapid
      'Solid', // Classical
      'ShortDash', // Correspondence
      'ShortDash', // Chess960
      'ShortDash', // KotH
      'ShortDot', // 3+
      'ShortDot', // Anti
      'ShortDot', // Atomic
      'Dash', // Horde
      'ShortDot', // Racing Kings
      'Dash', // Crazyhouse
      'Dash', // Puzzle
      'Dash', // Ultrabullet
    ].filter(indexFilter);
    window.Highcharts.stockChart(this, {
      yAxis: {
        title: noText,
      },
      credits: disabled,
      legend: disabled,
      colors: [
        '#56B4E9',
        '#0072B2',
        '#009E73',
        '#459F3B',
        '#F0E442',
        '#E69F00',
        '#D55E00',
        '#CC79A7',
        '#DF5353',
        '#66558C',
        '#99E699',
        '#FFAEAA',
        '#56B4E9',
        '#0072B2',
        '#009E73',
      ].filter(indexFilter),
      rangeSelector: {
        enabled: true,
        selected: 1,
        inputEnabled: false,
        labelStyle: {
          display: 'none',
        },
      },
      tooltip: {
        valueDecimals: 0,
      },
      xAxis: {
        title: noText,
        labels: disabled,
        lineWidth: 0,
        tickWidth: 0,
      },
      navigator: {
        baseSeries: perfIndex,
      },
      scrollbar: disabled,
      series: data
        .filter((v: any) => !singlePerfName || v.name === singlePerfName)
        .map((serie: any, i: number) => {
          const originalDatesAndRatings = serie.points.map((r: any) =>
            singlePerfName && serie.name !== singlePerfName ? [] : [Date.UTC(r[0], r[1], r[2]), r[3]],
          );
          return {
            name: serie.name,
            type: 'line',
            dashStyle: dashStyles[i],
            marker: disabled,
            data: smoothDates(originalDatesAndRatings),
          };
        }),
    });
  });
}
