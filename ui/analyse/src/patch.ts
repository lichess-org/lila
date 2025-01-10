import { type VNode, attributesModule, classModule, init } from 'snabbdom';

const sInit: (oldVnode: VNode | Element, vnode: VNode) => VNode = init([
  classModule,
  attributesModule,
]);

export default sInit;
