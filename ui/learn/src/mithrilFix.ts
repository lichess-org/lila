import m from 'mithril';

// Fix mithril 0.1.0's incorrect type definitions

type OneOrMany<T> = T | T[];

type Children<T> = OneOrMany<
  | string
  | number
  | null
  | undefined
  | _mithril.MithrilTrustedString
  | _mithril.MithrilVirtualElement<T>
  | _mithril.MithrilComponent<T>
>;

interface MithrilFixed extends _mithril.MithrilStatic {
  <T extends _mithril.MithrilController>(
    selector: string,
    attributes: Record<string, unknown>,
    children?: Children<T>
  ): _mithril.MithrilVirtualElement<T>;

  <T extends _mithril.MithrilController>(selector: string, children?: Children<T>): _mithril.MithrilVirtualElement<T>;
}

export default m as MithrilFixed;

export type MNode = _mithril.MithrilVirtualElement<_mithril.MithrilController>;
