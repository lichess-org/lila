import { Pane } from './pane';
import { Chart, PointElement, LinearScale, LineController, LineElement } from 'chart.js';
import { addPoint, asData, domain } from '../operator';
import { clamp, frag } from 'common';
import type { PaneArgs, OperatorInfo } from './devTypes';
import type { Operator } from '../types';

export class OperatorPane extends Pane {
  info: OperatorInfo;
  canvas: HTMLCanvasElement;
  chart: Chart;

  constructor(p: PaneArgs) {
    super(p);
    this.el.firstElementChild?.append(this.toggleGroup());
    this.canvas = document.createElement('canvas');
    const wrapper = frag<HTMLElement>(`<div class="chart-wrapper">`);
    wrapper.append(this.canvas);
    this.el.append(wrapper);
    this.host.cleanups.push(() => this.chart?.destroy());
    this.render();
  }

  setEnabled(enabled?: boolean): boolean {
    const canEnable = this.canEnable;
    if (canEnable) enabled ??= this.isRequired || (this.isDefined && !this.isDisabled);
    else enabled = false;
    if (enabled && !this.isDefined) {
      this.setProperty(structuredClone(this.info.value));
      this.render();
    }
    super.setEnabled(enabled);
    this.el.querySelectorAll('.chart-wrapper, .btn-rack')?.forEach(x => x.classList.toggle('none', !enabled));
    this.el.classList.toggle('none', !canEnable);
    if (enabled) this.host.bot.disabled.delete(this.id);
    else this.host.bot.disabled.add(this.id);
    return enabled;
  }

  update(e?: Event): void {
    if (!(e instanceof MouseEvent && e.target instanceof HTMLElement)) return;
    if (e.target.dataset.action && this.enabled) {
      if (e.target.classList.contains('active')) return;
      this.el.querySelector('.by.active')?.classList.remove('active');
      e.target.classList.add('active');
      this.paneValue.from = e.target.dataset.action as 'move' | 'score' | 'time';
      this.paneValue.data = [];
      this.render();
    }
    if (e.target instanceof HTMLCanvasElement) {
      const m = this.paneValue;
      const remove = this.chart.getElementsAtEventForMode(e, 'nearest', { intersect: true }, false);
      if (remove.length > 0 && remove[0].index > 0) {
        m.data.splice(remove[0].index - 1, 1);
      } else {
        const rect = e.target.getBoundingClientRect();

        const chartX = this.chart.scales.x.getValueForPixel(e.clientX - rect.left);
        const chartY = this.chart.scales.y.getValueForPixel(e.clientY - rect.top);
        if (!chartX || !chartY) return;
        addPoint(m, [clamp(chartX, domain(m)), clamp(chartY, m.range)]);
      }
      this.chart.data.datasets[0].data = asData(m);
      this.chart.update();
    }
  }

  get paneValue(): Operator {
    return this.getProperty() as Operator;
  }

  private render() {
    this.chart?.destroy();
    const m = this.paneValue;
    if (!m?.data) return;
    this.chart = new Chart(this.canvas.getContext('2d')!, {
      type: 'line',
      data: {
        datasets: [
          {
            data: asData(m),
            backgroundColor: 'rgba(75, 192, 192, 0.6)',
            pointRadius: 4,
            pointHoverBackgroundColor: 'rgba(220, 105, 105, 0.6)',
          },
        ],
      },
      options: {
        parsing: false,
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        scales: {
          x: {
            type: 'linear',
            min: domain(m).min,
            max: domain(m).max,
            reverse: m.from === 'time',
            ticks: getTicks(m),
            title: {
              display: true,
              color: '#555',
              text:
                m.from === 'move'
                  ? 'full moves'
                  : m.from === 'time'
                  ? 'think time'
                  : `outcome expectancy for ${this.host.bot.name.toLowerCase()}`,
            },
          },
          y: {
            min: m.range.min,
            max: m.range.max,
            title: {
              display: true,
              color: '#555',
              text: this.info.label,
            },
          },
        },
      },
    });
  }

  private toggleGroup() {
    const active = (this.paneValue ?? this.info.value).from;
    const by = frag<HTMLElement>(`<div class="btn-rack">
        <div data-action="move" class="by${active === 'move' ? ' active' : ''}">by move</div>
        <div data-action="score" class="by${active === 'score' ? ' active' : ''}">by score</div>
        <div data-action="time" class="by${active === 'time' ? ' active' : ''}">by time</div>
      </div>`);
    return by;
  }
}

function getTicks(o: Operator) {
  return o.from === 'time'
    ? {
        callback: (value: number) => {
          switch (value) {
            case -2:
              return '¼s';
            case -1:
              return '½s';
            case 0:
              return '1s';
            case 1:
              return '2s';
            case 2:
              return '4s';
            case 3:
              return '8s';
            case 4:
              return '15s';
            case 5:
              return '30s';
            case 6:
              return '1m';
            case 7:
              return '2m';
            case 8:
              return '4m';
            default:
              return '';
          }
        },
        maxTicksLimit: 11,
        stepSize: 1,
      }
    : undefined;
}

Chart.register(PointElement, LinearScale, LineController, LineElement);
