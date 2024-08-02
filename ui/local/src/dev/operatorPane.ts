import { Pane } from './pane';
import { Chart, PointElement, LinearScale, LineController, LineElement } from 'chart.js';
import { addPoint, asData, domain } from '../operator';
import { clamp } from 'common';
import type { PaneArgs, OperatorInfo } from './types';
import type { Operator } from '../types';

export class OperatorPane extends Pane {
  info: OperatorInfo;
  canvas: HTMLCanvasElement;
  chart: Chart;

  constructor(p: PaneArgs) {
    super(p);
    this.el.firstElementChild?.append(this.toggleGroup());
    this.canvas = document.createElement('canvas');
    const wrapper = $as<HTMLElement>(`<div class="chart-wrapper">`);
    wrapper.append(this.canvas);
    this.el.append(wrapper);
    this.host.cleanups.push(() => this.chart?.destroy());
    this.render();
  }

  setEnabled(enabled?: boolean): boolean {
    const canEnable = this.canEnable;
    if (canEnable) enabled ??= this.isRequired || (this.isDefined && !this.host.bot.disabled.has(this.id));
    else enabled = false;
    if (enabled && !this.isDefined) {
      this.setProperty(structuredClone(this.info.value));
      this.render();
    }
    super.setEnabled(enabled);
    this.el.querySelectorAll('.chart-wrapper, .btn-rack')?.forEach(x => x.classList.toggle('none', !enabled));
    this.el.classList.toggle('none', !canEnable);
    return enabled;
  }

  update(e?: Event): void {
    if (!(e instanceof MouseEvent && e.target instanceof HTMLElement)) return;
    if (e.target.dataset.click && this.enabled) {
      if (e.target.classList.contains('active')) return;
      this.el.querySelector('.by.active')?.classList.remove('active');
      e.target.classList.add('active');
      this.paneValue.from = e.target.dataset.click as 'move' | 'score' | 'time';
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
    console.log((this.host as any).wtf);
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
            title: {
              display: true,
              color: '#555',
              text:
                m.from === 'move'
                  ? 'full moves'
                  : m.from === 'time'
                  ? 'seconds remaining'
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
    const by = $as<HTMLElement>(`<div class="btn-rack">
        <div data-click="move" class="by${active === 'move' ? ' active' : ''}">by move</div>
        <div data-click="score" class="by${active === 'score' ? ' active' : ''}">by score</div>
        <div data-click="time" class="by${active === 'time' ? ' active' : ''}">by time</div>
      </div>`);
    return by;
  }
}

Chart.register(PointElement, LinearScale, LineController, LineElement);
