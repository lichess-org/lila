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

  test('clears the preload after the first blueprint is ready', () => {
    document.body.innerHTML = '';
    const el = document.createElement('div');
    el.innerHTML = '<span class="preload">preview</span>';
    document.body.appendChild(el);
    const snabbdomPatch = init([classModule, attributesModule]);
    const seen: string[] = [];
    const patch: Patch = (oldVnode, vnode) => {
      seen.push(el.innerHTML);
      return snabbdomPatch(oldVnode, vnode);
    };

    mountApp({
      el,
      patch,
      makeCtrl: () => ({}),
      view: () => {
        seen.push(el.innerHTML);
        return h('div', 'fresh');
      },
    });

    assert.deepEqual(seen, ['<span class="preload">preview</span>', '']);
    assert.equal(document.body.textContent, 'fresh');
    assert.equal(document.querySelector('.preload'), null);
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
