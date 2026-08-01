import { frag } from 'lib';
import { isTouchDevice, displayColumns } from 'lib/device';
import { licon } from 'lib/licon';
import type { LobbyShortcut } from 'lib/types';
import { domDialog, type Dialog, confirm } from 'lib/view';

import { ShortcutsCtrl, fitShortcut } from './shortcutsCtrl';

const shortcutIdMimeType = 'application/x-lichess-shortcut-id';

export async function initModule({
  ctrl,
  contextual,
}: {
  ctrl?: ShortcutsCtrl;
  contextual?: LobbyShortcut[];
}) {
  if (ctrl) ctrl.setup(contextual);
  else ctrl = new ShortcutsCtrl(undefined, contextual);
  const [touchDragPolyfill] = await Promise.all([
    isTouchDevice()
      ? import(site.asset.url('npm/drag-drop-touch.esm.min.js')).then(m => m.enableDragDropTouch)
      : undefined,
    ctrl.loaded,
  ]);
  const dlg = await domDialog({
    class: 'shortcuts-dialog',
    css: [{ hashed: 'lobby.shortcuts-dialog' }],
    show: true,
    noCloseButton: !isTouchDevice(),
    modal: !isTouchDevice(),
    focus: !isTouchDevice() ? '.desktop-only .save' : undefined,
    htmlText: $html`
      <button class="mobile-only button-empty reset" data-icon="${licon.ChasingArrows}"></button>
      <h2>Add to shortcuts</h2>
      <div class="shortcuts-view">
        <div class="scratch" aria-label="Available shortcuts"></div>
        <div class="shortcuts" aria-label="Chosen shortcuts">
          <div class="unavailable-slot" data-icon="${licon.NotAllowed}">
            <button class="mobile-only button button-text save">${i18n.site.save}</button>
          </div>
        </div>
      </div>
      <div class="desktop-only">
        <button class="button button-metal reset">${i18n.site.reset}</button>
        <button class="button button-empty button-red cancel">${i18n.site.cancel}</button>
        <button class="button save">${i18n.site.save}</button>
      </div>`,
    insert: [
      ...ctrl.scratch.map(s => renderShortcut(s, true)).map(node => ({ node, where: '.scratch' })),
      ...ctrl.configured.map(s => renderShortcut(s)).map(node => ({ node, where: '.shortcuts' })),
    ],
    onShow: dlg => {
      const dragFrom = dlg.view.querySelector<HTMLElement>('.shortcuts');
      const dropIn = dlg.view.querySelector<HTMLElement>('.shortcuts-view');
      touchDragPolyfill?.(dragFrom, dropIn, { dragImageOpacity: 0.9 });
    },
    actions: [
      { selector: '.save', result: 'save' },
      { selector: '.cancel', result: 'cancel' },
      { selector: '.reset', listener: (_, dlg) => reset(dlg, ctrl) },
      { selector: '.shortcuts .shortcut', listener: (e, dlg) => removeShortcut(e, dlg, ctrl) },
      { selector: '.scratch .shortcut', listener: placeShortcutHandler(ctrl) },
      { selector: '.shortcut', event: 'keydown', listener: keydownHandler(ctrl) },
      { selector: '[draggable="true"]', event: 'dragstart', listener: dragStart },
      {
        selector: '.slot, .shortcut, .scratch',
        event: ['dragover', 'drop'],
        listener: dragDropHandler(ctrl),
      },
    ],
  });
  if (dlg.returnValue === 'save') await ctrl.save();
}

async function reset(dlg: Dialog, ctrl: ShortcutsCtrl) {
  if (!(await confirm('Reset shortcuts?'))) return;
  ctrl.reset();
  dlg.view.querySelectorAll('.slot, .shortcut').forEach(el => el.remove());
  dlg.view.querySelector('.scratch')?.append(...ctrl.scratch.map(s => renderShortcut(s, true)));
  dlg.view.querySelector('.shortcuts')?.append(...ctrl.configured.map(s => renderShortcut(s)));
  dlg.updateActions();
}

function renderShortcut(s: LobbyShortcut | null, scratch = false): Element {
  if (!s) return frag('<div class="slot" aria-label="Empty shortcut slot">');

  const { scale, text } = scratch && displayColumns() === 1 ? fitShortcut(s, 18, 48) : fitShortcut(s);
  const el = frag<Element>($html`
    <div class="shortcut" tabindex="0" role="button" draggable="true" data-id="${s.id}"
         style="---scale: ${scale}"></div>`);
  if (s.iconUrl) el.append(frag(`<div class="icon"><img src="${s.iconUrl}" alt=""></div>`));
  el.append(
    ...([
      s.iconKey && frag(`<div class="icon"><i data-icon="${licon[s.iconKey]}"></i></div>`),
      s.iconMaskUrl &&
        frag(`<div class="icon"><div class="mask" style="---icon-mask:url(${s.iconMaskUrl})"></div></div>`),
      'perf' in s && frag(`<div class="clock">${s.id}</div>`),
      frag(`<div class="name">${'perf' in s ? s.perf : text}</div>`),
    ].filter(Boolean) as Node[]),
  );
  return el;
}

function dragStart(e: DragEvent) {
  const id = (e.currentTarget as HTMLElement).dataset.id!;
  if (!id || !e.dataTransfer) return;

  e.dataTransfer.setData(shortcutIdMimeType, id);
  e.dataTransfer.setData('text/plain', id);
  e.dataTransfer.effectAllowed = 'move';
}

function dragDropHandler(ctrl: ShortcutsCtrl) {
  return (e: DragEvent, dlg: Dialog) => {
    e.preventDefault();
    if (e.type !== 'drop' || !e.dataTransfer) return;

    const id = e.dataTransfer.getData(shortcutIdMimeType);
    if (!id) return;

    const target = e.currentTarget as HTMLElement;
    if (target.matches('.scratch')) return removeShortcut(target.dataset.id!, dlg, ctrl);

    let index = 0;
    for (let sib = target.previousElementSibling; sib; sib = sib.previousElementSibling) {
      if (sib.classList.contains('slot') || sib.classList.contains('shortcut')) index++;
    }
    if (ctrl.place(id, index)) transition(() => placeShortcut(id, index, dlg, ctrl));
  };
}

function placeShortcutHandler(ctrl: ShortcutsCtrl) {
  return (e: Event, dlg: Dialog) => {
    const id = (e.currentTarget as HTMLElement).dataset.id!;
    const insertAt = ctrl.configured.indexOf(null);
    if (ctrl.place(id, insertAt)) transition(() => placeShortcut(id, insertAt, dlg, ctrl));
  };
}

function keydownHandler(ctrl: ShortcutsCtrl) {
  return (e: KeyboardEvent, dlg: Dialog) => {
    const target = e.currentTarget as HTMLElement;
    if (e.key !== 'Enter' && e.key !== ' ') return;

    if (target.matches('.shortcuts .shortcut')) removeShortcut(target.dataset.id!, dlg, ctrl);
    else if (target.matches('.shortcut')) {
      const id = target.dataset.id!;
      const insertAt = ctrl.configured.indexOf(null);
      if (ctrl.place(id, insertAt)) transition(() => placeShortcut(id, insertAt, dlg, ctrl));
    }
  };
}

function removeShortcut(eventOrId: Event | string, dlg: Dialog, ctrl: ShortcutsCtrl) {
  const id = typeof eventOrId === 'string' ? eventOrId : (eventOrId.currentTarget as HTMLElement).dataset.id!;
  if (!ctrl.remove(id)) return;

  const from = dlg.view.querySelector(`.shortcut[data-id="${id}"]`);
  if (!from) return;

  const scratch = dlg.view.querySelector('.scratch')!;
  transition(() => {
    from.replaceWith(renderShortcut(null));
    scratch.insertBefore(from, scratch.children[ctrl.scratchIndexOf(id)]);
    dlg.updateActions();
  });
}

function placeShortcut(id: string, atIndex: number, dlg: Dialog, ctrl: ShortcutsCtrl) {
  const from = dlg.view.querySelector(`.shortcut[data-id="${id}"]`);
  if (!from) return;

  const activeGrid = dlg.view.querySelector('.shortcuts')!;
  const to = slotAt(activeGrid, Math.max(0, atIndex));
  if (!to) return;

  const scratch = from.closest('.scratch');
  if (scratch) {
    activeGrid.insertBefore(from, to);
    if (to.draggable) scratch.insertBefore(to, scratch.children[ctrl.scratchIndexOf(to.dataset.id!)]);
    else to.remove();
  } else {
    const swap = document.createElement('div');
    from.replaceWith(swap);
    to.replaceWith(from);
    swap.replaceWith(to);
  }
  dlg.updateActions();
}

function slotAt(el: Element, index: number): HTMLElement | null {
  for (const slot of el.querySelectorAll<HTMLElement>('.slot, .shortcut')) {
    if (index <= 0) return slot;
    index--;
  }
  return null;
}

function transition(update: () => void) {
  if (!document.startViewTransition) return update();
  // browsers don't currently provide a way to do view transitions with proper clipping when elements
  // migrate between containers. Due to temporary hoisting of transitioned elements to the body layer, they
  // are drawn outside their ancestor clip regions. To get around this, manually clip to the shortcuts-view
  // bounding box on the viewport itself. minor visual glitches can be expected on landscape mobile

  const rect = document.querySelector('.shortcuts-dialog .shortcuts-view')!.getBoundingClientRect();
  const root = document.documentElement;
  const right = window.innerWidth - rect.right;
  const bottom = window.innerHeight - rect.bottom;

  root.classList.add('shortcuts-transition');
  root.style.setProperty('---transition-clip', `inset(${rect.top}px ${right}px ${bottom}px ${rect.left}px)`);

  const cleanup = () => {
    root.classList.remove('shortcuts-transition');
    root.style.removeProperty('---transition-clip');
  };
  document.startViewTransition(update).finished.then(cleanup, cleanup);
}
