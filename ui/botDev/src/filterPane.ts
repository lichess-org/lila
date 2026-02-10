import { Pane } from './pane';
import { Chart, PointElement, LinearScale, LineController, LineElement } from 'chart.js';
import {
  addPoint,
  asData,
  filterFacetKeys,
  filterFacets,
  filterBys,
  type Filter,
  type FilterBy,
  type FilterFacetKey,
} from 'lib/bot/filter';
import { frag } from 'lib';
import { clamp } from 'lib/algo';
import type { PaneArgs, FilterInfo } from './devTypes';

type FacetToggle = { el: HTMLElement; input: HTMLInputElement };

export class FilterPane extends Pane {
  info: FilterInfo;
  graphEl: HTMLElement;
  graph: Chart;
  facets = {} as { [key in FilterFacetKey]: FacetToggle };

  constructor(p: PaneArgs) {
    super(p);
    if (!this.isDefined) this.setProperty(structuredClone(this.info.value));
    if (this.info.title && this.label) this.label.title = this.info.title;
    this.el.title = '';
    this.input = document.createElement('select');
    this.input.title = 'move / score / time function combinator';
    this.input.append(
      ...filterBys.map(c =>
        frag(`<option ${c === this.paneValue.by ? 'selected ' : ''}value="${c}">${c}</option>`),
      ),
    );
    this.input.addEventListener('change', e => {
      this.paneValue.by = (e.target as HTMLSelectElement).value as FilterBy;
      super.update();
    });
    this.label?.append(this.input);
    const tabs = frag<HTMLElement>(`<div class="btn-rack"></div>`);
    for (const facet of filterFacetKeys) {
      this.facets[facet] = this.makeFacet(facet);
      tabs.append(this.facets[facet].el);
    }
    this.el.firstElementChild?.append(tabs);
    this.graphEl = frag<HTMLElement>(`<div class="graph-wrapper"><canvas></canvas></div>`);
    this.el.append(this.graphEl);
    this.host.janitor.addCleanupTask(() => this.graph?.destroy());
  }

  setEnabled(enabled?: boolean): boolean {
    if (this.requirementsAllow) enabled ??= !this.isOptional || (this.isDefined && !this.isDisabled);
    else enabled = false;
    let enabledFacets = 0;
    for (const [facet, facetPane] of Object.entries(this.facets)) {
      facetPane.el.classList.toggle('active', enabled && facet === this.viewing && facetPane.input.checked);
      if (facetPane.input.checked) enabledFacets++;
    }
    this.input?.classList.toggle('none', enabledFacets < 2);
    super.setEnabled(enabled);
    this.renderGraph();
    return enabled;
  }

  private toggleFacet = (facet: FilterFacetKey, checked?: boolean): boolean => {
    if (checked) this.facets[facet].input.checked = checked;
    else checked = this.facets[facet].input.checked;
    if (checked) {
      this.host.editing().disabled.delete(`${this.id}_${facet}`);
      this.paneValue[facet] ??= [];
    } else this.host.editing().disabled.add(`${this.id}_${facet}`);
    return checked;
  };

  private makeFacet(facet: FilterFacetKey): { input: HTMLInputElement; el: HTMLElement } {
    const input = frag<HTMLInputElement>(`<input type="checkbox">`);
    const el = frag<HTMLElement>(
      `<div data-facet="${facet}" class="${this.viewing === facet ? 'active ' : ''}facet" title="${
        tooltips[facet]
      }">${facet}</div>`,
    );
    input.addEventListener('change', () => {
      if (this.toggleFacet(facet)) this.viewing = facet;
      else if (this.viewing === facet) this.viewing = undefined;
      super.update();
    });
    input.checked =
      Boolean(this.paneValue?.[facet]) && !this.host.editing().disabled.has(`${this.id}_${facet}`);
    if (input.checked && !this.viewing) this.viewing = facet;
    el.addEventListener('click', e => {
      if (e.target instanceof HTMLInputElement || !(e.target instanceof HTMLElement)) return;
      if (this.viewing === facet) this.viewing = undefined;
      else {
        this.viewing = facet;
        this.toggleFacet(facet, true);
      }
      if (this.facets[facet].input.checked && this.viewing === facet) {
        e.target.closest('.facet')?.classList.add('active');
        this.paneValue[facet] ??= [];
      }
      super.update(e);
    });
    el.prepend(input);
    return { input, el };
  }

  update(e?: Event): void {
    if (!(e instanceof MouseEvent && this.viewing && e.target instanceof HTMLCanvasElement)) return;

    const f = this.paneValue;
    const data = this.paneValue[this.viewing];
    const remove = this.graph.getElementsAtEventForMode(e, 'nearest', { intersect: true }, false);
    if (remove.length > 0 && remove[0].index > 0) {
      data?.splice(remove[0].index - 1, 1);
    } else {
      const rect = e.target.getBoundingClientRect();

      const graphX = this.graph.scales.x.getValueForPixel(e.clientX - rect.left);
      const graphY = this.graph.scales.y.getValueForPixel(e.clientY - rect.top);
      if (!graphX || !graphY) return;
      addPoint(f, this.viewing, [clamp(graphX, filterFacets[this.viewing].domain), clamp(graphY, f.range)]);
    }
    this.graph.data.datasets[0].data = asData(f, this.viewing);
    this.graph.update();
  }

  get paneValue(): Filter {
    return this.getProperty() as Filter;
  }

  get viewing(): FilterFacetKey | undefined {
    return this.host.editing().viewing?.get(this.id) as FilterFacetKey | undefined;
  }

  set viewing(f: FilterFacetKey | undefined) {
    if (f) this.host.editing().viewing.set(this.id, f);
    else this.host.editing().viewing.delete(this.id);
  }

  private renderGraph() {
    this.graph?.destroy();
    this.graphEl.classList.remove('hidden', 'none');
    const f = this.paneValue;
    if (
      !this.viewing ||
      !f?.[this.viewing] ||
      this.host.editing().disabled.has(`${this.id}_${this.viewing}`)
    ) {
      this.graphEl.classList.add(Object.values(this.facets).some(x => x.input.checked) ? 'hidden' : 'none');
      return;
    }
    this.graph = new Chart(this.graphEl.querySelector<HTMLCanvasElement>('canvas')!.getContext('2d')!, {
      type: 'line',
      data: {
        datasets: [
          {
            data: asData(f, this.viewing),
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
            min: filterFacets[this.viewing].domain.min,
            max: filterFacets[this.viewing].domain.max,
            //reverse: this.viewing === 'time',
            ticks: getTicks(this.viewing),
            title: {
              display: true,
              color: '#555',
              text:
                this.viewing === 'move'
                  ? 'full moves'
                  : this.viewing === 'time'
                    ? 'think time'
                    : `outcome expectancy for ${this.host.editing().name.toLowerCase()}`,
            },
          },
          y: {
            min: f.range.min,
            max: f.range.max,
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
}

function getTicks(facet: FilterFacetKey) {
  return facet === 'time'
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

const tooltips: { [key in FilterFacetKey]: string } = {
  move: 'vary the filter parameter by number of full moves since start of game',
  score: `vary the filter parameter by current outcome expectancy for bot`,
  time: 'vary the filter parameter by think time in seconds per move',
};

Chart.register(PointElement, LinearScale, LineController, LineElement);
