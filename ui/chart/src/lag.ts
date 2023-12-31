import {
  ArcElement,
  Chart,
  ChartConfiguration,
  ChartDataset,
  ChartType,
  DoughnutController,
  Title,
  Tooltip,
} from 'chart.js';

declare module 'chart.js' {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface PluginOptionsByType<TType extends ChartType> {
    needle?: {
      value: number;
    };
  }
}

Chart.register(DoughnutController, ArcElement, Title, Tooltip);

export async function initModule() {
  lichess.StrongSocket.firstConnect.then(() => lichess.socket.send('moveLat', true));
  $('.meter canvas').each(function (this: HTMLCanvasElement, index) {
    const dataset: ChartDataset<'doughnut'>[] = [
      {
        data: [500, 150, 100],
        backgroundColor: ['#55bf3b', '#dddf0d', '#df5353'],
        hoverBorderColor: 'white',
        hoverBorderWidth: 4,
        circumference: 180,
        rotation: 270,
      },
    ];
    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: ['0-500', '500-650', '650-750'],
        datasets: dataset,
      },
      options: {
        plugins: {
          title: {
            display: true,
            text: (index ? 'Ping' : 'Server latency') + ' in milliseconds',
            padding: { top: 50 },
          },
          tooltip: {
            callbacks: {
              title: item => (item[0].dataIndex == 0 ? 'Good' : item[0].dataIndex == 1 ? 'Ok' : 'Bad'),
              label: item => item.label,
            },
          },
          needle: {
            value: 0,
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
    if (index == 0)
      lichess.pubsub.on('socket.in.mlat', (d: number) => {
        chart.options.plugins!.needle!.value = Math.min(750, d);
        chart.update();
      });
    else {
      setInterval(function () {
        const v = Math.round(lichess.socket.averageLag);
        if (v) chart.options.plugins!.needle!.value = Math.min(750, v);
        chart.update();
      }, 1000);
    }
  });
}
