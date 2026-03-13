import { h, type VNode } from 'snabbdom';

import { defined, notNull } from 'lib';
import { formatClockTimeVerbal } from 'lib/game/clock/clockView';
import * as licon from 'lib/licon';
import type { TreePath } from 'lib/tree/types';
import { iconTag, type MaybeVNode, type MaybeVNodes } from 'lib/view';

import type AnalyseCtrl from '../ctrl';

interface ClockOpts {
  centis: number | undefined;
  active: boolean;
  cls: string;
  showTenths: boolean;
  pause: boolean;
  empty?: boolean;
  editable?: boolean;
  isEditing?: boolean;
  editValue?: string;
  onClockClick?: () => void;
  onEditInput?: (value: string) => void;
  onEditKeydown?: (e: KeyboardEvent) => void;
  onEditBlur?: () => void;
  hasError?: boolean;
  shakeTrigger?: number;
}

import { formatClockFromCentis } from './clockParse';

const pad2 = (num: number): string => (num < 10 ? '0' : '') + num;

export default function renderClocks(ctrl: AnalyseCtrl, path: TreePath): [VNode, VNode] | undefined {
  const study = ctrl.study;
  const node = ctrl.tree.nodeAtPath(path);
  const whitePov = ctrl.bottomIsWhite();
  const parentClock = ctrl.tree.getParentClock(node, path);
  // isWhiteTurn = white to move (ply 0,2,4...). [0] = white, [1] = black (playerBars uses clocks[white?0:1]).
  const isWhiteTurn = node.ply % 2 === 0;
  const centis: Array<number | undefined> = (
    isWhiteTurn ? [parentClock, node.clock] : [node.clock, parentClock]
  ).map(c => (defined(c) && c < 0 ? undefined : c));

  const hasAnyClock = centis.some(notNull);
  const isStudy = !!study;
  const isRelay = !!study?.relay;
  const editState = study ? study.clockEditState(path) : { canEdit: false as const };
  // When no clock data: show nothing except on mainline when editable, so user can add times
  if (!hasAnyClock && !(editState.canEdit && editState.editableSlot)) return undefined;

  if (study?.clockEdit && study.clockEdit.path !== path) {
    setTimeout(() => study.commitClockEdit({ whenInvalid: 'closeNow' }), 0);
  }

  const lastMoveAt = study
    ? study.isClockTicking(path)
      ? study.relay?.lastMoveAt(study.vm.chapterId)
      : undefined
    : ctrl.autoplay.lastMoveAt;

  if (lastMoveAt && hasAnyClock) {
    const spent = (Date.now() - lastMoveAt) / 10;
    // Tick the side to move: white to move → index 0, black to move → index 1
    const i = isWhiteTurn ? 0 : 1;
    if (centis[i]) centis[i] = Math.max(0, centis[i] - spent);
  }

  const showTenths = !study?.relay;
  const pause = !!ctrl.study?.isRelayAwayFromLive();

  if (!isStudy || (isRelay && !hasAnyClock)) {
    return [
      renderClock({
        centis: centis[0],
        active: isWhiteTurn,
        cls: whitePov ? 'bottom' : 'top',
        showTenths,
        pause,
      }),
      renderClock({
        centis: centis[1],
        active: !isWhiteTurn,
        cls: whitePov ? 'top' : 'bottom',
        showTenths,
        pause,
      }),
    ];
  }

  const clockEdit = study.clockEdit;
  const slotForIndex = (i: number) => (i === 0 ? (whitePov ? 'bottom' : 'top') : whitePov ? 'top' : 'bottom');
  const renderOpts = (index: number): ClockOpts => {
    const cls = index === 0 ? (whitePov ? 'bottom' : 'top') : whitePov ? 'top' : 'bottom';
    const empty = !defined(centis[index]) && centis[index] !== 0;
    const editable = !!editState.editableSlot && editState.editableSlot === slotForIndex(index);
    const isEditing = !!clockEdit && clockEdit.path === path && clockEdit.slot === slotForIndex(index);
    return {
      centis: centis[index],
      // Active (blue) = side to move
      active: node.ply > 0 && index === (isWhiteTurn ? 0 : 1),
      cls,
      showTenths,
      pause,
      empty: empty && !isEditing,
      editable,
      isEditing,
      editValue: isEditing ? clockEdit.value : undefined,
      onClockClick:
        editable && !clockEdit
          ? () => {
              study.openClockEdit(
                slotForIndex(index),
                path,
                defined(centis[index]) || centis[index] === 0 ? formatClockFromCentis(centis[index]) : '',
              );
            }
          : undefined,
      onEditInput: isEditing ? v => study.clockEdit!.setValue(v) : undefined,
      onEditKeydown: isEditing
        ? (e: KeyboardEvent) => {
            if (e.key === 'Enter') {
              study.commitClockEdit();
            } else if (e.key === 'Escape') {
              study.closeClockEdit();
            }
          }
        : undefined,
      onEditBlur: isEditing ? () => study.commitClockEdit({ whenInvalid: 'closeAfterFeedback' }) : undefined,
      hasError: isEditing && !!clockEdit.error,
      shakeTrigger: isEditing ? clockEdit.shakeTrigger : undefined,
    };
  };

  return [renderClock(renderOpts(0)), renderClock(renderOpts(1))];
}

const renderClock = (opts: ClockOpts): VNode => {
  const content = site.blindMode ? [clockContentNvui(opts)] : clockContent(opts);
  return h(
    'div.analyse__clock.' + opts.cls,
    {
      class: {
        active: opts.active,
        'analyse__clock--empty': !!opts.empty,
        'analyse__clock--editable': !!(opts.editable && !opts.isEditing),
        'analyse__clock--editing': !!opts.isEditing,
      },
      attrs: opts.editable && !opts.isEditing ? { title: i18n.study.addOrEditClockTime } : {},
      on: opts.onClockClick
        ? {
            click: (e: MouseEvent) => {
              if (!(e.target as HTMLElement).matches('input')) {
                e.preventDefault();
                opts.onClockClick!();
              }
            },
          }
        : undefined,
    },
    content,
  );
};

function clockContent(opts: ClockOpts): MaybeVNodes {
  if (opts.isEditing && opts.editValue !== undefined) {
    const shakeTrigger = opts.shakeTrigger ?? 0;
    return [
      h('input.analyse__clock-input', {
        class: {
          'analyse__clock-input--error': !!opts.hasError,
          'analyse__clock-input--shake':
            !!opts.hasError && opts.shakeTrigger != null && opts.shakeTrigger > 0,
        },
        attrs: {
          type: 'text',
          value: opts.editValue,
          placeholder: i18n.study.clockTimePlaceholder,
          title: i18n.study.clockTimeFormat,
          'data-cy': 'clock-input',
        },
        shakeTrigger,
        hook: {
          insert: vnode => {
            (vnode.elm as HTMLInputElement).focus();
            (vnode.elm as HTMLInputElement).select();
          },
          postpatch: (oldVnode, vnode) => {
            const oldTrigger = (oldVnode.data as { shakeTrigger?: number })?.shakeTrigger;
            const newTrigger = (vnode.data as { shakeTrigger?: number })?.shakeTrigger;
            if (oldTrigger !== newTrigger && newTrigger != null && newTrigger > 0) {
              const el = vnode.elm as HTMLElement;
              el.classList.remove('analyse__clock-input--shake');
              void el.offsetHeight;
              requestAnimationFrame(() => el.classList.add('analyse__clock-input--shake'));
            }
          },
        },
        on: {
          input: (e: Event) => opts.onEditInput!((e.target as HTMLInputElement).value),
          keydown: (e: KeyboardEvent) => opts.onEditKeydown!(e),
          blur: () => opts.onEditBlur?.(),
        },
      }),
    ];
  }
  if (!opts.centis && opts.centis !== 0) {
    if (opts.editable) {
      return [h('span.analyse__clock-placeholder', {}, ['--:--'])];
    }
    // Missing non-editable clock shows as a dash.
    return ['-'];
  }
  const date = new Date(opts.centis * 10),
    millis = date.getUTCMilliseconds(),
    sep = ':',
    baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  const timeNodes =
    !opts.showTenths || opts.centis >= 360000
      ? [Math.floor(opts.centis / 360000) + sep + baseStr]
      : opts.centis >= 6000
        ? [baseStr]
        : [baseStr, h('tenths', '.' + Math.floor(millis / 100).toString())];
  const pauseNodes = opts.pause ? [iconTag(licon.Pause)] : [];
  return [...pauseNodes, ...timeNodes];
}

const clockContentNvui = (opts: ClockOpts): MaybeVNode =>
  !opts.centis && opts.centis !== 0 ? 'None' : formatClockTimeVerbal(opts.centis * 10);
