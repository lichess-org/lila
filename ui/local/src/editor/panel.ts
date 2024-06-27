import { PaneArgs, EditorHost, SelectorInfo, PaneInfo, ObjectSelector, AnyType } from './types';
import { Pane } from './pane';
import { Setting, NumberSetting } from './setting';
import { Mappings, Mapping } from '../types';
import { getSchemaDefault } from './schema';
import { Chart, PointElement, LinearScale, LineController, LineElement, Tooltip } from 'chart.js';
import { maxChars } from './util';
import { addPoint, asData, domain } from '../mapping';

Chart.register(PointElement, LinearScale, Tooltip, LineController, LineElement);

export class SelectorPanel extends Setting {
  info: SelectorInfo;
  canvas: HTMLCanvasElement;
  chart: Chart;

  constructor(p: PaneArgs) {
    super(p);
    this.div.classList.add('panel');
    const constT = this.toggleGroup();
    this.div.append(constT);
    this.canvas = document.createElement('canvas');
    const wrapper = $as<HTMLElement>(`<div class="chart-wrapper">`);
    wrapper.append(this.canvas);
    this.div.append(wrapper);
    this.host.cleanups.push(() => this.chart?.destroy());
    this.renderMapping();
  }

  toggleGroup() {
    const active = (this.paneValue ?? this.info.value).from;
    const by = $as<HTMLElement>(`<div class="btn-rack">
        <div class="by${active === 'move' ? ' active' : ''}">by move</div>
        <div class="by${active === 'score' ? ' active' : ''}">by score</div>
      </div>`);
    if (this.label) by.prepend(this.label);
    return by;
  }
  setEnabled(enabled = this.getProperty() !== undefined) {
    if (enabled && !this.autoEnable()) enabled = false;
    if (enabled && !this.getProperty()) {
      this.setProperty(structuredClone(this.info.value));
      this.renderMapping();
    }
    super.setEnabled(enabled);
    this.canvas.style.display = enabled ? 'block' : 'none';
  }
  update(e: Event) {
    if (!(e.target instanceof HTMLElement && e instanceof MouseEvent)) return;
    if (e.target.classList.contains('by') && this.enabled) {
      if (e.target.classList.contains('active')) return;
      this.div.querySelector('.by.active')?.classList.remove('active');
      e.target.classList.add('active');
      this.paneValue.from = this.info.value.from === 'score' ? 'move' : 'score';
      this.paneValue.data = [];
      this.renderMapping();
    }
    if (e.target.nodeName === 'CANVAS') {
      const m = this.paneValue;
      const remove = this.chart.getElementsAtEventForMode(e, 'nearest', { intersect: true }, false);
      if (remove.length > 0 && remove[0].index > 0) {
        m.data.splice(remove[0].index - 1, 1);
      } else {
        const rect = (e.target as HTMLElement).getBoundingClientRect();

        const chartX = this.chart.scales.x.getValueForPixel(e.clientX - rect.left);
        const chartY = this.chart.scales.y.getValueForPixel(e.clientY - rect.top);
        if (!chartX || !chartY) return;
        addPoint(m, { x: chartX, y: chartY });
      }
      this.chart.data.datasets[0].data = asData(m);
      this.chart.update();
    }
    this.host.update();
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
              text: m.from === 'move' ? 'Moves' : 'Score',
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
