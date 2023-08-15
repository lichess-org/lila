declare const cash: ((selector?: Selector, context?: Context | Cash) => Cash) & CashStatic;

declare type MapCallback<T> = (this: T, index: number, ele: T) => Ele;
declare type EachCallback<T> = (this: T, index: number, ele: T) => any;

declare type falsy = undefined | null | false | 0 | '';
declare type Ele = Window | Document | HTMLElement | Element | Node;
declare type EleLoose = HTMLElement & Element & Node;

declare type Selector = falsy | string | HTMLCollection | NodeList | Ele | Ele[] | ArrayLike<Ele> | Cash;
declare type Comparator = string | Ele | Cash | ((this: EleLoose, index: number, ele: EleLoose) => boolean);
declare type Context = Document | HTMLElement | Element;
declare type EventCallback = { (event: any, data?: any): any; guid?: number };

interface CashStatic {
  fn: Cash;
  unique<T>(arr: ArrayLike<T>): ArrayLike<T>;
  each<T>(arr: ArrayLike<T>, callback: EachCallback<T>): void;
  guid: number;
  isWindow(x: any): x is Window;
  // eslint-disable-next-line @typescript-eslint/ban-types
  isFunction(x: any): x is Function;
  isNumeric(x: any): boolean;
  isArray(x: any): x is Array<any>;
  parseHTML(html: string): EleLoose[];
}

declare class Cash {
  length: number;
  constructor(selector?: Selector, context?: Context | Cash);
  init(selector?: Selector, context?: Context | Cash): Cash;
  [index: number]: EleLoose | undefined;
  splice(start: number, deleteCount?: number): EleLoose[];
  splice(start: number, deleteCount: number, ...items: Ele[]): EleLoose[];
  slice(start?: number, end?: number): Cash;
  map(callback: MapCallback<EleLoose>): Cash;
  each(callback: EachCallback<EleLoose>): this;
  removeProp(prop: string): this;
  prop(prop: string): any;
  prop(prop: string, value: any): this;
  prop(props: Record<string, any>): this;
  get(): EleLoose[];
  get(index: number): EleLoose | undefined;
  eq(index: number): Cash;
  first(): Cash;
  last(): Cash;
  filter(comparator?: Comparator): Cash;
  hasClass(cls: string): boolean;
  removeAttr(attrs: string): this;
  attr(): undefined;
  attr(attrs: string): string | null;
  attr(attrs: string, value: string): this;
  attr(attrs: Record<string, string>): this;
  toggleClass(classes: string, force?: boolean): this;
  addClass(classes: string): this;
  removeClass(classes?: string): this;
  add(selector: Selector, context?: Context): Cash;
  css(prop: string): string | undefined;
  css(prop: string, value: number | string): this;
  css(props: Record<string, number | string>): this;
  data(): Record<string, any> | undefined;
  data(name: string): any;
  data(name: string, value: any): this;
  data(datas: Record<string, any>): this;
  innerWidth(): number | undefined;
  innerHeight(): number | undefined;
  outerWidth(includeMargins?: boolean): number;
  outerHeight(includeMargins?: boolean): number;
  width(): number;
  width(value: number | string): this;
  height(): number;
  height(value: number | string): this;
  toggle(force?: boolean): this;
  hide(): this;
  show(): this;
  off(): this;
  off(events: string): this;
  off(events: Record<string, EventCallback>): this;
  off(events: string, callback: EventCallback): this;
  off(events: string, selector: string, callback: EventCallback): this;
  on(events: Record<string, EventCallback>): this;
  on(events: Record<string, EventCallback>, selector: string): this;
  on(events: Record<string, EventCallback>, data: any): this;
  on(events: Record<string, EventCallback>, selector: string | null | undefined, data: any): this;
  on(events: string, callback: EventCallback): this;
  on(events: string, selector: string, callback: EventCallback): this;
  on(events: string, data: any, callback: EventCallback): this;
  on(
    events: string,
    selector: string | null | undefined,
    data: any,
    callback: EventCallback,
    _one?: boolean,
  ): this;
  one(events: Record<string, EventCallback>): this;
  one(events: Record<string, EventCallback>, selector: string): this;
  one(events: Record<string, EventCallback>, data: any): this;
  one(events: Record<string, EventCallback>, selector: string | null | undefined, data: any): this;
  one(events: string, callback: EventCallback): this;
  one(events: string, selector: string, callback: EventCallback): this;
  one(events: string, data: any, callback: EventCallback): this;
  one(events: string, selector: string | null | undefined, data: any, callback: EventCallback): this;
  // eslint-disable-next-line @typescript-eslint/ban-types
  ready(callback: Function): this;
  trigger(event: Event | string, data?: any): this;
  val(): string | string[];
  val(value: string | string[]): this;
  clone(): this;
  detach(comparator?: Comparator): this;
  empty(): this;
  html(): string;
  html(html: string): this;
  remove(comparator?: Comparator): this;
  text(): string;
  text(text: string): this;
  unwrap(): this;
  offset(): undefined | { top: number; left: number };
  offsetParent(): Cash;
  position(): undefined | { top: number; left: number };
  children(comparator?: Comparator): Cash;
  contents(): Cash;
  find(selector: string): Cash;
  after(...selectors: Selector[]): this;
  append(...selectors: Selector[]): this;
  appendTo(selector: Selector): this;
  before(...selectors: Selector[]): this;
  insertAfter(selector: Selector): this;
  insertBefore(selector: Selector): this;
  prepend(...selectors: Selector[]): this;
  prependTo(selector: Selector): this;
  replaceWith(selector: Selector): this;
  replaceAll(selector: Selector): this;
  wrapAll(selector?: Selector): this;
  wrap(selector?: Selector): this;
  wrapInner(selector?: Selector): this;
  has(selector: string | Node): Cash;
  is(comparator?: Comparator): boolean;
  next(comparator?: Comparator, _all?: boolean, _until?: Comparator): Cash;
  nextAll(comparator?: Comparator): Cash;
  nextUntil(until?: Comparator, comparator?: Comparator): Cash;
  not(comparator?: Comparator): Cash;
  parent(comparator?: Comparator): Cash;
  index(selector?: Selector): number;
  closest(comparator?: Comparator): Cash;
  parents(comparator?: Comparator, _until?: Comparator): Cash;
  parentsUntil(until?: Comparator, comparator?: Comparator): Cash;
  prev(comparator?: Comparator, _all?: boolean, _until?: Comparator): Cash;
  prevAll(comparator?: Comparator): Cash;
  prevUntil(until?: Comparator, comparator?: Comparator): Cash;
  siblings(comparator?: Comparator): Cash;
}
/* hacks */
interface CashStatic {
  (selector: Selector, context?: Element | Cash): Cash;
}
declare module 'cash' {
  export = $;
}
declare const $: CashStatic;
/* export default cash; */
/* export { Cash, CashStatic, Ele as Element, Selector, Comparator, Context }; */
/* end hacks */
