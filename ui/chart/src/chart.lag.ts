import {
  type ChartConfiguration,
  type ChartDataset,
  type ChartType,
  ArcElement,
  Chart,
  DoughnutController,
  Title,
} from 'chart.js';
import dataLabels from 'chartjs-plugin-datalabels';
import { fontColor, fontFamily } from './index';
import { pubsub } from 'lib/pubsub';
import { wsSend, wsAverageLag } from 'lib/socket';

declare module 'chart.js' {
  // oxlint-disable-next-line no-unused-vars
  interface PluginOptionsByType<TType extends ChartType> {
    needle?: {
      value: number;
    };
  }
}

Chart.register(DoughnutController, ArcElement, dataLabels, Title);
Chart.defaults.font = fontFamily();

const v = {
  server: -1,
  network: -1,
};

export async function initModule(): Promise<void> {
  pubsub.after('socket.hasConnected').then(() => wsSend('moveLat', true));
  $('.meter canvas').each(function (this: HTMLCanvasElement, index) {
    const colors = ['#55bf3b', '#dddf0d', '#df5353'];
    const dataset: ChartDataset<'doughnut'>[] = [
      {
        data: index ? [500, 150, 100] : [300, 250, 200],
        backgroundColor: colors,
        hoverBackgroundColor: colors,
        borderColor: '#d9d9d9',
        borderWidth: 3,
        circumference: 180,
        rotation: 270,
      },
    ];
    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: index ? ['0-500', '500-650', '650-750'] : ['0-300', '300-550', '550-750'],
        datasets: dataset,
      },
      options: {
        events: [],
        plugins: {
          title: {
            display: true,
            text: '',
            padding: { top: 100 },
            color: fontColor,
          },
          needle: {
            value: index ? v.network : v.server,
          },
          datalabels: {
            color: 'black',
            formatter: (_, ctx) => ctx.chart.data.labels![ctx.dataIndex],
          },
        },
      },
      plugins: [
        {
          id: 'needle',
          afterDatasetDraw(chart, _args, _opts) {
            const ctx = chart.ctx;
            ctx.save();
            const data = chart.getDatasetMeta(0).data[0] as ArcElement;
            const first = chart.data.datasets[0].data[0] as number;
            let dest = data.circumference / Math.PI / first;
            dest = dest * (chart.options.plugins?.needle?.value ?? 1);
            const outer = data.outerRadius;
            ctx.translate(data.x, data.y);
            ctx.rotate(Math.PI * (dest + 1.5));
            ctx.beginPath();
            ctx.fillStyle = '#838382';
            ctx.moveTo(0 - 10, 0);
            ctx.lineWidth = 1;
            ctx.lineTo(0, -outer);
            ctx.lineTo(0 + 10, 0);
            ctx.lineTo(0 - 10, 0);
            ctx.fill();

            ctx.beginPath();
            ctx.arc(0, 0, 9, 0, (Math.PI / 180) * 360, false);
            ctx.fill();

            ctx.restore();
          },
        },
      ],
    };
    const chart = new Chart(this, config);
    if (index === 0)
      pubsub.on('socket.in.mlat', (d: number) => {
        v.server = d;
        if (v.server <= 0) return;
        chart.options.plugins!.needle!.value = Math.min(750, v.server);
        chart.options.plugins!.title!.text = makeTitle(index, v.server);
        updateAnswer();
      });
    else {
      setInterval(function () {
        v.network = Math.round(wsAverageLag());
        if (v.network <= 0) return;
        chart.options.plugins!.needle!.value = Math.min(750, v.network);
        chart.options.plugins!.title!.text = makeTitle(index, v.network);
        updateAnswer();
      }, 1000);
    }
    const updateAnswer = () => {
      if (v.server === -1 || v.network === -1) return;
      const c = v.server <= 100 && v.network <= 500 ? 'nope-nope' : v.server <= 100 ? 'nope-yep' : 'yep';
      $('.lag .answer span')
        .addClass('none')
        .parent()
        .find('.' + c)
        .removeClass('none');
      chart.update();
    };
  });
}

const makeTitle = (index: number, lat: number) => [
  (index ? 'Ping' : 'Server latency') + ' in milliseconds',
  `${lat}`,
];
