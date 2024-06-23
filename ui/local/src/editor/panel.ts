import { PaneArgs, EditorHost, MappingInfo, BaseInfo, ObjectSelector } from './types';
import { Pane } from './pane';
import { Mapping } from '../types';
import { getSchemaDefault } from './schema';

abstract class Panel extends Pane {
  info: MappingInfo;
  constructor(args: PaneArgs) {
    super(args);
  }
  get inputValue(): Mapping {
    return this.getProperty();
  }
  get id() {
    return this.info.id!;
  }
  get enabled() {
    return this.host.bot.disabled.has(this.id);
  }
  setEnabled(enabled?: boolean | 'refresh') {
    if (!this.enabledCheckbox) return;
    this.enabledCheckbox.checked = !!enabled;
  }
}
//import * as ch from 'chart.js';
import { Chart, PointElement, LinearScale, LineController, LineElement, Tooltip } from 'chart.js';
import { maxChars } from './util';
import { addPoint, asData, domain } from '../mapping';

/*

  showPanel = (dlg: Dialog, action: Action, e: Event) => {
    document.querySelectorAll('.selectable').forEach(el => el.classList.remove('selected'));
    const setting = this.settings.byEvent(e);
    if (!setting) return;
    const el = this.view.querySelector('.edit-panel') as HTMLElement;
    el.classList.remove('none');
    el.dataset.showing = setting.id;
    setting.select();
    document.querySelectorAll('.btn-rack__btn').forEach(el => el.classList.remove('active'));
    (e.target as HTMLElement).classList.add('active');
  };

        <span class="btn-rack">
        <span class="btn-rack__btn byMoves">Moves</span>
        <span class="btn-rack__btn byScore">Score</span>
      </span>
      
*/
export class MappingPanel extends Panel {
  info: MappingInfo;
  getProperty(sel?: ObjectSelector[]): Mapping {
    return this.info.value;
  }

  setProperty(value: Mapping) {}
  constructor(p: PaneArgs) {
    super(p);

    const rack = $as<HTMLElement>(`<span class="btn-rack">`);
    this.div.append(rack);
    const constant = $as<HTMLInputElement>(`<input type="text" data-type="number" class="btn-rack">`);
    constant.maxLength = 4;
    constant.style.maxWidth = `calc(5ch + 1.5em)`;
    rack.append(constant);
    const moves = $as<HTMLElement>(`<span class="btn-rack__btn byMoves">Moves</span>`);
    rack.append(moves);
    const score = $as<HTMLElement>(`<span class="btn-rack__btn byScore">Score</span>`);
    rack.append(score);
  }

  select() {
    this.div.classList.add('selected');
    const maybeFrozen = this.getProperty(['bot', 'default', 'schema']) as Mapping;
    const mapping = Object.isFrozen(maybeFrozen) ? structuredClone(maybeFrozen) : maybeFrozen;
    this.setProperty(mapping);
    this.info.value = mapping;
    const panel = this.host.view.querySelector('.edit-panel') as HTMLElement;
    panel.classList.remove('none');
    panel.dataset.showing = this.id;
    const canvas = panel.querySelector('canvas') as HTMLCanvasElement;
    canvas.innerHTML = '';
    renderMapping(canvas, this.info, this.host);
  }
  update(e: Event) {
    if (e.target instanceof HTMLInputElement) {
      const v = Number(e.target.value);
      if (isNaN(v) || v < this.info.value.range.min || v > this.info.value.range.max) {
        // bad, select moves / score
      } else {
        // good, deactivate moves and score
      }
    }
    this.host.update();
  }
  click(e: MouseEvent) {
    if (e.target instanceof HTMLElement) {
      if (e.target.classList.contains('byMoves')) {
        this.info.value.data.from = 'move';
        this.update(e);
      } else if (e.target.classList.contains('byScore')) {
        this.info.value.data.from = 'score';
        this.update(e);
      }
    }
  }
  get inputValue(): Mapping {
    return this.getProperty();
  }
}

Chart.register(PointElement, LinearScale, Tooltip, LineController, LineElement);

let chart: Chart;
let clickHandler: (e: MouseEvent) => void;

export function renderMapping(canvas: HTMLCanvasElement, info: MappingInfo, host: EditorHost) {
  const m = info.value;
  if (chart) {
    chart.destroy();
    canvas.removeEventListener('click', clickHandler);
  }
  clickHandler = (e: MouseEvent) => {
    const remove = chart.getElementsAtEventForMode(e, 'nearest', { intersect: true }, false);
    if (remove.length > 0 && remove[0].index > 0) {
      if (m.data.from === 'const') throw new Error('Unexpected number');
      m.data.to.splice(remove[0].index - 1, 1);
    } else {
      const rect = (e.target as HTMLElement).getBoundingClientRect();

      const chartX = chart.scales.x.getValueForPixel(e.clientX - rect.left);
      const chartY = chart.scales.y.getValueForPixel(e.clientY - rect.top);
      if (!chartX || !chartY) return;
      addPoint(m, { x: chartX, y: chartY });
    }
    chart.data.datasets[0].data = asData(m);
    chart.update();
    host.update();
  };
  canvas.addEventListener('click', clickHandler);
  chart = new Chart(canvas.getContext('2d')!, {
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
            text: m.data.from === 'move' ? 'Moves' : 'Score',
          },
        },
        y: {
          beginAtZero: true,
          min: m.range.min,
          max: m.range.max,
          title: {
            display: true,
            text: info.label,
          },
        },
      },
    },
  });
}
