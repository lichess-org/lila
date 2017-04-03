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

    startComputation(): void;
    endComputation(): void;
  }

  type Config = (el: Element, isUpdate: boolean, ctx: any, vdom: VirtualElement) => void;

  interface Attributes {
    class?: string;
    className?: string;
    config?: Config;
    key?: string | number;
    [property: string]: any;
  }

  interface VirtualElement {
  }
}


declare module "mithril" {
  const m: Mithril.Static;
  export = m;
}
