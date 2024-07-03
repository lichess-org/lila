import { PaneArgs, EditorHost, MoveSelectorInfo, PaneInfo, ObjectSelector, AnyType } from './types';
import { Setting } from './setting';
import { Mapping } from '../types';
import { Chart, PointElement, LinearScale, LineController, LineElement, Tooltip } from 'chart.js';
import { addPoint, asData, domain } from '../mapping';
import { alert } from 'common/dialog';
import { clamp } from 'common';

Chart.register(PointElement, LinearScale, /*Tooltip,*/ LineController, LineElement);

export class MoveSelector extends Setting {
  info: MoveSelectorInfo;
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
    this.renderMapping();
  }

  toggleGroup() {
    const active = (this.paneValue ?? this.info.value).from;
    const by = $as<HTMLElement>(`<div class="btn-rack">
        <div data-click="move" class="by${active === 'move' ? ' active' : ''}">by move</div>
        <div data-click="score" class="by${active === 'score' ? ' active' : ''}">by score</div>
      </div>`);
    //if (this.label) by.prepend(this.label);
    return by;
  }

  setEnabled(enabled?: boolean) {
    const canEnable = this.canEnable();
    if (enabled && !canEnable) {
      alert(`Cannot enable ${this.info.label} because of unmet preconditions: ${this.requires.join(', ')}`);
      enabled = false;
    } else enabled ??= canEnable && (this.info.required || !this.host.bot.disabled.has(this.id));

    if (enabled && !this.getProperty()) {
      this.setProperty(structuredClone(this.info.value));
      this.renderMapping();
    }
    this.el.querySelectorAll('.chart-wrapper, .btn-rack')?.forEach(x => x.classList.toggle('none', !enabled));
    this.el.classList.toggle('none', !canEnable); //!enabled && this.info.required === true);
    super.setEnabled(enabled);
  }

  update(e: Event) {
    if (!(e.target instanceof HTMLElement && e instanceof MouseEvent)) return;
    if (e.target.dataset.click && this.enabled) {
      if (e.target.classList.contains('active')) return;
      this.el.querySelector('.by.active')?.classList.remove('active');
      e.target.classList.add('active');
      this.paneValue.from = e.target.dataset.click as 'move' | 'score';
      this.paneValue.data = [];
      this.renderMapping();
    }
    if (e.target instanceof HTMLCanvasElement) {
      const m = this.paneValue;
      const remove = this.chart.getElementsAtEventForMode(e, 'nearest', { intersect: true }, false);
      if (remove.length > 0 && remove[0].index > 0) {
        m.data.splice(remove[0].index - 1, 1);
      } else {
        const rect = (e.target as HTMLElement).getBoundingClientRect();

        const chartX = this.chart.scales.x.getValueForPixel(e.clientX - rect.left);
        const chartY = this.chart.scales.y.getValueForPixel(e.clientY - rect.top);
        if (!chartX || !chartY) return;
        addPoint(m, { x: clamp(chartX, domain(m)), y: clamp(chartY, m.range) });
      }
      this.chart.data.datasets[0].data = asData(m);
      this.chart.update();
    }
  }
  get paneValue(): Mapping {
    return this.getProperty() as Mapping;
  }

  private renderMapping() {
    const m = this.paneValue;
    this.chart?.destroy();
    if (!m?.data) return;
    this.chart = new Chart(this.canvas.getContext('2d')!, {
      type: 'line',
      data: {
        datasets: [
          {
            //label: 'My Dataset',
            data: asData(m),
            backgroundColor: 'rgba(75, 192, 192, 0.6)',
          },
        ],
      },
      options: {
        parsing: false,
        responsive: true,
        maintainAspectRatio: false,
        animation: false /*{ duration: 100 },*/,
        layout: {
          padding: {
            left: 16,
            right: 16,
            top: 16,
          },
        },
        scales: {
          x: {
            type: 'linear',
            beginAtZero: true,
            min: domain(m).min,
            max: domain(m).max,
            title: {
              display: true,
              text:
                m.from === 'move'
                  ? 'full moves'
                  : `outcome expectancy for ${this.host.bot.name.toLowerCase()}`,
            },
          },
          y: {
            beginAtZero: true,
            min: m.range.min,
            max: m.range.max,
            title: {
              display: true,
              text: this.info.label,
            },
          },
        },
      },
    });
  }
}
