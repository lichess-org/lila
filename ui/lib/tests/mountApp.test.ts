import assert from 'node:assert/strict';
import { describe, test } from 'node:test';
import { attributesModule, classModule, h, init, type VNode } from 'snabbdom';

import { mountApp } from '../src/view/mountApp';

type Patch = (oldVnode: VNode | Element | DocumentFragment, vnode: VNode) => VNode;

describe('mountApp', () => {
  test('mounts once and repaints through the injected redraw', () => {
    document.body.innerHTML = '';
    const el = document.createElement('div');
    document.body.appendChild(el);
    let count = 0;
    const ctrl = mountApp({
      el,
      makeCtrl: redraw => ({
        redraw,
        increment(): void {
          count++;
          this.redraw();
        },
      }),
      view: () => h('span', String(count)),
    });

    assert.equal(document.body.textContent, '0');
    ctrl.increment();
    assert.equal(document.body.textContent, '1');
  });

  test('uses the supplied patch function', () => {
    document.body.innerHTML = '';
    const el = document.createElement('div');
    document.body.appendChild(el);
    const snabbdomPatch = init([classModule, attributesModule]);
    let calls = 0;
    const patch: Patch = (oldVnode, vnode) => {
      calls++;
      return snabbdomPatch(oldVnode, vnode);
    };

    mountApp({ el, patch, makeCtrl: () => ({}), view: () => h('span', 'ok') });

    assert.equal(calls, 1);
    assert.equal(document.body.textContent, 'ok');
  });
});
