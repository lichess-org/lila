import { schema, infoKeys } from './schema';
import { Pane, SelectSetting, RangeSetting, TextareaSetting, TextSetting, NumberSetting } from './pane';
import { FilterPane } from './filterPane';
import { SoundEventPane } from './soundEventPane';
import { BooksPane } from './booksPane';
import type { EditDialog } from './editDialog';
import type { PaneInfo, InfoKey } from './devTypes';
import type { ActionListener, Action } from 'lib/view';

export class Panes {
  byId: Record<string, Pane> = {};

  byEl(el: Element): Pane | undefined {
    while (el && this.byId[el.id] === undefined) el = el.parentElement!;
    return el ? this.byId[el.id] : undefined;
  }

  byEvent(e: Event): Pane | undefined {
    return e.target instanceof Element ? this.byEl(e.target) : undefined;
  }

  add(pane: Pane): void {
    this.byId[pane.el.id] = pane;
  }

  forEach(cb: (value: Pane, key: string) => void): void {
    Object.keys(this.byId).forEach(key => cb(this.byId[key], key));
  }

  dependsOn(idOrGroup: string): Pane[] {
    return [...new Set(Object.values(this.byId).filter(pane => pane.requires.includes(idOrGroup)))];
  }

  get actions(): Action[] {
    return [
      { selector: '[data-type]', event: 'input', listener: this.updateProperty },
      { selector: '[data-action]', listener: this.updateProperty },
      { selector: 'canvas', listener: this.updateProperty },
      { selector: '.toggle', event: 'change', listener: this.toggleEnabled },
    ];
  }

  private toggleEnabled: ActionListener = e => {
    const pane = this.byEvent(e)!;
    pane.setProperty(pane.paneValue);
    pane.setEnabled((e.target as HTMLInputElement).checked);
    (e.target as HTMLInputElement).checked = pane.enabled;
    pane.host.update();
  };

  private updateProperty: ActionListener = e => {
    const pane = this.byEvent(e)!;
    pane.update(e);
    pane.host.update();
  };
}

export function buildFromSchema(host: EditDialog, path: string[], parent?: Pane): Pane {
  const id = path.join('_');
  const iter = path.reduce<any>((acc, key) => acc[key], schema());
  const s = makePane(host, { id, ...iter }, parent);
  for (const key of Object.keys(iter).filter(k => !infoKeys.includes(k as InfoKey))) {
    s.el.appendChild(buildFromSchema(host, [...path, key], s).el);
  }
  return s;
}

function makePane(host: EditDialog, info: PaneInfo, parent?: Pane): Pane {
  const p = { host, info, parent };
  switch (info?.type) {
    case 'select':
      return new SelectSetting(p);
    case 'range':
      return new RangeSetting(p);
    case 'textarea':
      return new TextareaSetting(p);
    case 'text':
      return new TextSetting(p);
    case 'number':
      return new NumberSetting(p);
    case 'books':
      return new BooksPane(p);
    case 'soundEvent':
      return new SoundEventPane(p);
    case 'filter':
      return new FilterPane(p);
    default:
      return new Pane(p);
  }
}
