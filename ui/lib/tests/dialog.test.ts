import assert from 'node:assert/strict';
import { before, describe, test } from 'node:test';

import { pubsub } from '../src/pubsub';
import { domDialog } from '../src/view/dialog';

describe('domDialog', () => {
  before(() => {
    (site as any).asset = {
      loadCssPath: async () => {},
      loadCss: async () => {},
      removeCssPath: () => {},
      removeCss: () => {},
    };
    (globalThis as any).MutationObserver = window.MutationObserver;
    (window as any).matchMedia = (globalThis as any).matchMedia;

    const dialogPrototype = Object.getPrototypeOf(document.createElement('dialog'));
    dialogPrototype.show = function () {
      this.open = true;
      this.setAttribute('open', '');
    };
    dialogPrototype.showModal = dialogPrototype.show;
    dialogPrototype.close = function (returnValue = '') {
      this.returnValue = returnValue;
      this.open = false;
      this.removeAttribute('open');
      this.dispatchEvent(new window.Event('close'));
    };

    pubsub.complete('polyfill.dialog');
  });

  test('single keeps only the latest dialog open after concurrent shows', async () => {
    document.body.replaceChildren();

    for (let i = 0; i < 8; i++)
      void domDialog({
        htmlText: `<p>${i}</p>`,
        class: 'setting-popup',
        noCloseButton: true,
        show: true,
        single: 'setting-popup',
      });

    await new Promise(resolve => setTimeout(resolve, 0));

    const openPopups = [...document.querySelectorAll('dialog[open] .setting-popup')];
    assert.equal(openPopups.length, 1);
    assert.equal(openPopups[0]?.textContent, '7');

    document.querySelector<HTMLDialogElement>('dialog[open]')?.close('ok');
  });
});
