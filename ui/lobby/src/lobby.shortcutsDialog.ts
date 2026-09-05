import { frag } from 'lib';
import { isTouchDevice, displayColumns } from 'lib/device';
import type { LobbyShortcut } from 'lib/types';
import { domDialog, type Dialog, confirm } from 'lib/view';

import { ShortcutsCtrl, fitShortcut } from './shortcutsCtrl';

const shortcutIdMimeType = 'application/x-lichess-shortcut-id';

interface ShortcutDialogCtx {
  ctrl: ShortcutsCtrl;
  drag?: { id: string; atIndex?: number };
}

type ShortcutDialog = Dialog<ShortcutDialogCtx>;

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
    ctx: { ctrl },
    class: 'shortcuts-dialog',
    css: [{ hashed: 'lobby.shortcuts-dialog' }],
    show: true,
    noCloseButton: !isTouchDevice(),
    modal: !isTouchDevice(),
    focus: !isTouchDevice() ? '.desktop-only .save' : undefined,
    htmlText: $html`
      <button class="mobile-only button-empty reset">
        <span class="svg-icon icon-ChasingArrows"></span>
      </button>
      <h2>Add to shortcuts</h2>
      <div class="shortcuts-view">
        <div class="scratch" aria-label="Available shortcuts"></div>
        <div class="shortcuts" aria-label="Chosen shortcuts">
          <div class="unavailable-slot">
            <span class="svg-icon icon-notAllowed"></span>
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
      ...ctrl.scratch.map(s => renderShortcut(s, true)).map(nodes => ({ nodes, selector: '.scratch' })),
      ...ctrl.configured.map(renderSlot).map(nodes => ({ nodes, selector: '.shortcuts' })),
    ],
    onShow: dlg => {
      const dragFrom = dlg.view.querySelector<HTMLElement>('.shortcuts');
      const dropIn = dlg.view.querySelector<HTMLElement>('.shortcuts-view');
      touchDragPolyfill?.(dragFrom, dropIn, { dragImageOpacity: 0.9 });
    },
    actions: [
      { selector: '.save', result: 'save' },
      { selector: '.cancel', result: 'cancel' },
      { selector: '.reset', listener: reset },
      { selector: '.shortcuts .shortcut', listener: (e, dlg) => removeShortcut(e, dlg) },
      { selector: '.scratch .shortcut', listener: placeShortcut },
      { selector: '.shortcut', event: 'keydown', listener: keydown },
      { selector: '[draggable="true"]', event: 'dragstart', listener: dragStart },
      { selector: '[draggable="true"]', event: 'dragend', listener: dragEnd },
      { selector: '.slot', event: ['dragover', 'drop'], listener: dragOverGrid },
      { selector: '.scratch', event: ['dragover', 'drop'], listener: dragOverScratch },
    ],
  });
  if (dlg.returnValue === 'save') await dlg.ctx.ctrl.save();
}

async function reset(_: Event, dlg: ShortcutDialog) {
  if (!(await confirm('Reset shortcuts?'))) return;
  const { ctrl } = dlg.ctx;
  delete dlg.ctx.drag;
  ctrl.reset();
  dlg.view.querySelectorAll('.shortcut').forEach(el => el.remove());
  dlg.view.querySelector('.scratch')?.append(...ctrl.scratch.map(s => renderShortcut(s, true)));
  ctrl.configured.forEach(
    (s, i) => s && dlg.view.querySelector(`.slot[data-index="${i}"]`)?.append(renderShortcut(s)),
  );
  dlg.updateActions();
}

function renderSlot(s: LobbyShortcut | null, index: number): Element {
  const slot = frag<Element>(`<div class="slot" data-index="${index}" aria-label="Shortcut slot">`);
  if (s) slot.append(renderShortcut(s));
  return slot;
}

function renderShortcut(s: LobbyShortcut, scratch = false): Element {
  const { scale, text } = scratch && displayColumns() === 1 ? fitShortcut(s, 18, 48) : fitShortcut(s);
  const el = frag<Element>($html`
    <div class="shortcut" tabindex="0" role="button" draggable="true" data-id="${s.id}"
         style="---scale: ${scale}; view-transition-name: shortcut-${CSS.escape(s.id)}"></div>`);
  if (s.iconUrl) el.append(frag(`<div class="icon"><img src="${s.iconUrl}" alt=""></div>`));
  el.append(
    ...([
      s.iconKey && frag(`<div class="icon"><span class="svg-icon icon-${s.iconKey}"></span></div>`),
      s.iconMaskUrl &&
        frag(`<div class="icon"><div class="mask" style="---icon-mask:url(${s.iconMaskUrl})"></div></div>`),
      'perf' in s && frag(`<div class="clock">${s.id}</div>`),
      frag(`<div class="name">${'perf' in s ? s.perf : text}</div>`),
    ].filter(Boolean) as Node[]),
  );
  return el;
}

function dragStart(e: DragEvent, dlg: ShortcutDialog) {
  const id = (e.currentTarget as HTMLElement).dataset.id!;
  if (!id || !e.dataTransfer) return;

  dlg.ctx.drag = { id };
  e.dataTransfer.setData(shortcutIdMimeType, id);
  e.dataTransfer.setData('text/plain', id);
  e.dataTransfer.effectAllowed = 'move';
}

function dragEnd(_: DragEvent, dlg: ShortcutDialog) {
  const drag = dlg.ctx.drag;
  if (!drag) return;

  delete dlg.ctx.drag;
  transition(() => {
    restorePreview(drag.id, drag.atIndex, dlg);
    dlg.updateActions();
  });
}

function dragOverGrid(e: DragEvent, dlg: ShortcutDialog) {
  e.preventDefault();
  const drag = dlg.ctx.drag;
  if (!drag) return;

  const target = e.currentTarget as HTMLElement;
  const index = Number(target.dataset.index);
  const atIndex = index === dlg.ctx.ctrl.configuredIndexOf(drag.id) ? undefined : index;
  if (e.type === 'drop') {
    if (drag.atIndex !== atIndex) {
      restorePreview(drag.id, drag.atIndex, dlg);
      if (atIndex !== undefined) moveShortcut(drag.id, atIndex, dlg, true);
    }
    dlg.ctx.ctrl.place(drag.id, index);
    delete dlg.ctx.drag;
    dlg.updateActions();
  } else if (drag.atIndex !== atIndex) {
    const previousIndex = drag.atIndex;
    drag.atIndex = atIndex;
    transition(() => {
      restorePreview(drag.id, previousIndex, dlg);
      if (atIndex !== undefined) moveShortcut(drag.id, atIndex, dlg, true);
      dlg.updateActions();
    });
  }
}

function dragOverScratch(e: DragEvent, dlg: ShortcutDialog) {
  e.preventDefault();
  const drag = dlg.ctx.drag;
  if (!drag) return;

  if (e.type === 'dragover') {
    if (drag.atIndex === undefined) return;
    const previousIndex = drag.atIndex;
    delete drag.atIndex;
    transition(() => {
      restorePreview(drag.id, previousIndex, dlg);
      dlg.updateActions();
    });
  } else {
    delete dlg.ctx.drag;
    transition(() => {
      restorePreview(drag.id, drag.atIndex, dlg);
      removeShortcut(drag.id, dlg, false);
      dlg.updateActions();
    });
  }
}

function placeShortcut(e: Event, dlg: ShortcutDialog) {
  const id = (e.currentTarget as HTMLElement).dataset.id!;
  const insertAt = dlg.ctx.ctrl.configured.indexOf(null);
  if (dlg.ctx.ctrl.place(id, insertAt))
    transition(() => {
      moveShortcut(id, insertAt, dlg);
      dlg.updateActions();
    });
}

function keydown(e: KeyboardEvent, dlg: ShortcutDialog) {
  const target = e.currentTarget as HTMLElement;
  if (e.key !== 'Enter' && e.key !== ' ') return;

  if (target.matches('.shortcuts .shortcut')) removeShortcut(target.dataset.id!, dlg);
  else if (target.matches('.shortcut')) placeShortcut(e, dlg);
}

function removeShortcut(eventOrId: Event | string, dlg: ShortcutDialog, animate = true) {
  const id = typeof eventOrId === 'string' ? eventOrId : (eventOrId.currentTarget as HTMLElement).dataset.id!;
  if (!dlg.ctx.ctrl.remove(id)) return;

  const from = dlg.view.querySelector(`.shortcut[data-id="${id}"]`);
  if (!from) return;

  const scratch = dlg.view.querySelector('.scratch')!;
  const update = () => {
    scratch.insertBefore(from, scratch.children[dlg.ctx.ctrl.scratchIndexOf(id)]);
    dlg.updateActions();
  };
  if (animate) transition(update);
  else update();
}

function moveShortcut(id: string, atIndex: number, dlg: ShortcutDialog, preview = false) {
  const from = dlg.view.querySelector(`.shortcut[data-id="${id}"]`);
  if (!from) return;

  const activeGrid = dlg.view.querySelector('.shortcuts')!;
  const to = activeGrid.querySelector<HTMLElement>(`.slot[data-index="${Math.max(0, atIndex)}"]`);
  if (!to) return;

  const scratch = from.closest('.scratch');
  if (scratch) {
    const displaced = to.firstElementChild as HTMLElement | null;
    to.append(from);
    if (displaced) {
      let scratchIndex = dlg.ctx.ctrl.scratchIndexOf(displaced.dataset.id!);
      if (preview && dlg.ctx.ctrl.scratchIndexOf(id) < scratchIndex) scratchIndex--;
      scratch.insertBefore(displaced, scratch.children[scratchIndex]);
    }
  } else {
    const displaced = to.firstElementChild;
    const fromSlot = from.parentElement!;
    to.append(from);
    if (displaced) fromSlot.append(displaced);
  }
}

function restorePreview(id: string, atIndex: number | undefined, dlg: ShortcutDialog) {
  if (atIndex === undefined) return;

  const from = dlg.view.querySelector<HTMLElement>(`.slot > .shortcut[data-id="${id}"]`);
  if (!from) return;

  const fromIndex = dlg.ctx.ctrl.configuredIndexOf(id);
  if (!Number.isNaN(fromIndex)) return moveShortcut(id, fromIndex, dlg);

  const scratch = dlg.view.querySelector('.scratch')!;
  const displaced = dlg.ctx.ctrl.configured[atIndex];
  const to =
    displaced && dlg.view.querySelector<HTMLElement>(`.scratch > .shortcut[data-id="${displaced.id}"]`);
  if (to) from.replaceWith(to);
  else from.remove();
  scratch.insertBefore(from, scratch.children[dlg.ctx.ctrl.scratchIndexOf(id)]);
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
    const transitionCount = Number(root.dataset.transitionCount) - 1;
    if (transitionCount) {
      root.dataset.transitionCount = String(transitionCount);
      return;
    }
    delete root.dataset.transitionCount;
    root.classList.remove('shortcuts-transition');
    root.style.removeProperty('---transition-clip');
  };
  const viewTransition = document.startViewTransition(update);
  root.dataset.transitionCount = String(Number(root.dataset.transitionCount ?? 0) + 1);
  viewTransition.finished.then(cleanup, cleanup);
}
