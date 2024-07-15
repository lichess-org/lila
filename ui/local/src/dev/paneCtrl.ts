import { schema, primitiveKeys } from './schema';
import { Pane, SelectSetting, RangeSetting, TextareaSetting, TextSetting, NumberSetting } from './pane';
import { OperatorPane } from './operatorPane';
import { BooksPane } from './booksPane';
import type { PaneHost, PaneInfo, AnyKey } from './types';
import type { ActionListener, Action } from 'common/dialog';

export class PaneCtrl {
  byId: { [id: string]: Pane } = {};

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
    return Object.values(this.byId).filter(pane => pane.requires.includes(idOrGroup));
  }

  get actions(): Action[] {
    return [
      { selector: '[data-type]', event: 'input', listener: this.updateProperty },
      { selector: '[data-click]', listener: this.updateProperty },
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

export function buildFromSchema(host: PaneHost, path: string[], parent?: Pane): Pane {
  const id = path.join('_');
  const iter = path.reduce<any>((acc, key) => acc[key], schema);
  const s = buildFromInfo(host, { id, ...iter }, parent);
  for (const key of Object.keys(iter).filter(k => !primitiveKeys.includes(k as AnyKey))) {
    s.el.appendChild(buildFromSchema(host, [...path, key], s).el);
  }
  return s;
}

function buildFromInfo(host: PaneHost, info: PaneInfo, parent?: Pane): Pane {
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
    case 'operator':
      return new OperatorPane(p);
    default:
      return new Pane(p);
  }
}
/*
function require(requireId: string) {
  const [id, conf] = requireId.split(/=|\^=|$=|<|>|<=|>=/).map(x => x.trim());
  const op = conf ? requireId.match(/=|\^=|$=|<|>|<=|>=/)![0] : undefined;
  // stretch goal - support advanced requirements
}
  */
