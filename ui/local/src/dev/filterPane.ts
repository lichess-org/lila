import { Pane } from './pane';
import { Chart, PointElement, LinearScale, LineController, LineElement } from 'chart.js';
import { addPoint, asData, domain } from '../filter';
import { frag } from 'common';
import { clamp } from 'common/algo';
import type { PaneArgs, FilterInfo } from './devTypes';
import type { Filter } from '../types';

export class FilterPane extends Pane {
  info: FilterInfo;
  canvas: HTMLCanvasElement;
  chart: Chart;

  constructor(p: PaneArgs) {
    super(p);
    if (this.info.title && this.label) this.label.title = this.info.title;
    this.el.title = '';
    this.el.firstElementChild?.append(this.toggleGroup());
    this.canvas = document.createElement('canvas');
    const wrapper = frag<HTMLElement>(`<div class="chart-wrapper">`);
    wrapper.append(this.canvas);
    this.el.append(wrapper);
    this.host.chartJanitor.addCleanupTask(() => this.chart?.destroy());
    this.render();
  }

  setEnabled(enabled?: boolean): boolean {
    if (this.requirementsAllow) enabled ??= !this.isOptional || (this.isDefined && !this.isDisabled);
    else enabled = false;
    if (enabled && !this.isDefined) {
      this.setProperty(structuredClone(this.info.value));
      this.render();
    }
    super.setEnabled(enabled);
    this.el.querySelectorAll('.chart-wrapper, .btn-rack')?.forEach(x => x.classList.toggle('none', !enabled));
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
      this.paneValue.by = e.target.dataset.action as 'move' | 'score' | 'time';
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

  get paneValue(): Filter {
    return this.getProperty() as Filter;
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
            reverse: m.by === 'time',
            ticks: getTicks(m),
            title: {
              display: true,
              color: '#555',
              text:
                m.by === 'move'
                  ? 'full moves'
                  : m.by === 'time'
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
    const active = (this.paneValue ?? this.info.value).by;
    const by = frag<HTMLElement>(`<div class="btn-rack">
        <div data-action="move" class="by${active === 'move' ? ' active' : ''}" title="${
          tooltips.byMove
        }">by move</div>
        <div data-action="score" class="by${active === 'score' ? ' active' : ''}" title="${
          tooltips.byScore
        }">by score</div>
        <div data-action="time" class="by${active === 'time' ? ' active' : ''}" title="${
          tooltips.byTime
        }">by time</div>
      </div>`);
    return by;
  }
}

function getTicks(o: Filter) {
  return o.by === 'time'
    ? {
        callback: (value: number) => ticks[value] ?? '',
        maxTicksLimit: 11,
        stepSize: 1,
      }
    : undefined;
}

const ticks: Record<number, string> = {
  '-2': '¼s',
  '-1': '½s',
  0: '1s',
  1: '2s',
  2: '4s',
  3: '8s',
  4: '15s',
  5: '30s',
  6: '1m',
  7: '2m',
  8: '4m',
};

const tooltips = {
  byMove: 'vary the filter parameter by number of full moves since start of game',
  byScore: `vary the filter parameter by current outcome expectancy for bot`,
  byTime: 'vary the filter parameter by think time in seconds per move',
};

Chart.register(PointElement, LinearScale, LineController, LineElement);
