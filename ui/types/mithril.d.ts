declare namespace Mithril {
  interface Property<T> {
    (): T;
    (v: T): void;
  }

  type Renderable = Array<VirtualElement | string | null | undefined> |
                    VirtualElement | string | null | undefined;

  interface Static {
    (tag: string): VirtualElement;
    (tag: string, attrs: Attributes): VirtualElement;
    (tag: string, attrs: Attributes, ...children: Renderable[]): VirtualElement;
    (tag: string, ...children: Renderable[]): VirtualElement;

    prop<T>(d: T): Property<T>;

    module<T>(el: Element, module: Module<T>): void;

    startComputation(): void;
    endComputation(): void;

    redraw: Redraw;
  }

  type Config = (el: Element, isUpdate: boolean, ctx: any, vdom: VirtualElement) => void;

  interface Attributes {
    class?: string;
    className?: string;
    config?: Config;
    key?: string | number;
    [property: string]: any;
  }

  interface Module<T> {
    controller(): T;
    view(ctrl: T): Renderable;
  }

  type RedrawStrategy = 'all' | 'diff' | 'none';

  interface Redraw {
    (forceSync?: boolean): void;
    strategy(strategy: RedrawStrategy): void;
  }

  interface VirtualElement {
  }
}


declare module "mithril" {
  const m: Mithril.Static;
  export = m;
}
