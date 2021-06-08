import ApexCharts from 'apexcharts';

const light = $('body').hasClass('light');
const gridC = light ? '#dddddd' : '#333333';

const conf = (title: string, xaxis: Date[]) => ({
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
    background: 'transparent',
  },
  // series: data.reports.series,
  xaxis: {
    type: 'datetime',
    categories: xaxis,
  },
  yaxis: {
    opposite: true,
  },
  legend: {
    position: 'top',
  },
  stroke: {
    width: xaxis.length > 50 ? 1 : 2,
  },
  grid: {
    borderColor: gridC,
  },
});

export function activity(data: any) {
  const extend = {
    chart: {
      height: Math.round(window.innerHeight * 0.4),
    },
  };
  new ApexCharts(
    document.querySelector('.mod-activity .chart-reports'),
    merge(
      {
        ...conf('Closed reports', data.common.xaxis),
        series: data.reports.series,
      },
      extend
    )
  ).render();

  new ApexCharts(
    document.querySelector('.mod-activity .chart-actions'),
    merge(
      {
        ...conf('Mod actions', data.common.xaxis),
        series: data.actions.series,
      },
      extend
    )
  ).render();
}

export function queues(data: any) {
  const $grid = $('.chart-grid');
  data.rooms.forEach((room: any) => {
    const cfg = merge(
      {
        ...conf(room.name, data.common.xaxis),
        series: room.series.map(s => ({
          ...s,
          name: `Score: ${s.name}`,
        })),
      },
      {
        chart: {
          type: 'bar',
          stacked: true,
        },
        colors: ['#03A9F4', '#4CAF50', '#F9CE1D', '#FF9800'],
      }
    );
    console.log(cfg);
    new ApexCharts($('<div>').appendTo($grid)[0], cfg).render();
  });
}

function merge(base: any, extend: any): void {
  for (const key in extend) {
    if (isObject(base[key]) && isObject(extend[key])) merge(base[key], extend[key]);
    else base[key] = extend[key];
  }
  return base;
}

function isObject(o: unknown): boolean {
  return typeof o === 'object';
}
