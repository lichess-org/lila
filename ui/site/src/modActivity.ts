import ApexCharts from 'apexcharts';

export default function (data: any) {
  const light = $('body').hasClass('light');

  const conf = (title: string) => ({
    title: {
      text: title,
    },
    theme: {
      mode: light ? 'light' : 'dark',
    },
    chart: {
      type: 'line',
      zoom: {
        enabled: false,
      },
      animations: {
        enabled: false,
      },
      height: Math.round(window.innerHeight * 0.4),
      background: 'transparent',
    },
    // series: data.reports.series,
    xaxis: {
      type: 'datetime',
      categories: data.common.xaxis,
    },
    yaxis: {
      opposite: true,
    },
    legend: {
      position: 'top',
    },
    stroke: {
      width: data.common.xaxis.length > 50 ? 1 : 2,
    },
  });

  const reports = new ApexCharts(document.querySelector('.mod-activity .chart-reports'), {
    ...conf('Closed reports'),
    series: data.reports.series,
  });
  reports.render();

  const actions = new ApexCharts(document.querySelector('.mod-activity .chart-actions'), {
    ...conf('Mod actions'),
    series: data.actions.series,
  });
  actions.render();
}
