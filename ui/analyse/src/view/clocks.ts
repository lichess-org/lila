import { h, type VNode } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import { defined, notNull } from 'lib';
import * as licon from 'lib/licon';
import { iconTag, type MaybeVNode, type MaybeVNodes } from 'lib/view';
import { formatClockTimeVerbal } from 'lib/game/clock/clockView';
import type { TreePath } from 'lib/tree/types';

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
}

import { parseTimeToCentis, formatClockFromCentis } from './clockParse';

const isClearClockValue = (s: string): boolean => s.trim() === '-' || s.trim() === '--';

const pad2 = (num: number): string => (num < 10 ? '0' : '') + num;

export default function renderClocks(ctrl: AnalyseCtrl, path: TreePath): [VNode, VNode] | undefined {
  const study = ctrl.study;
  const node = ctrl.tree.nodeAtPath(path);
  const whitePov = ctrl.bottomIsWhite();
  const parentClock = ctrl.tree.getParentClock(node, path);
  // isWhiteTurn = white to move (ply 0,2,4...). [0] = white, [1] = black (playerBars uses clocks[white?0:1]).
  const isWhiteTurn = node.ply % 2 === 0;
  // At initial position (no move selected): show no clocks at all.
  if (node.ply === 0) {
    return undefined;
  }
  const centis: Array<number | undefined> = (
    isWhiteTurn ? [parentClock, node.clock] : [node.clock, parentClock]
  ).map(c => (defined(c) && c < 0 ? undefined : c));
  // After the first move (ply 1), the other player hasn't moved yet — never show their clock.
  if (node.ply === 1) {
    centis[1] = undefined;
  }

  const hasAnyClock = centis.some(notNull);
  const isStudy = !!study;
  const isRelay = !!study?.relay;
  const canEdit =
    isStudy &&
    !isRelay &&
    path === ctrl.path &&
    study.vm.mode.write &&
    study.members.canContribute() &&
    ctrl.tree.pathIsMainline(path);
  // When no clock data: show nothing except on mainline when editable, so user can add times
  if (!hasAnyClock && !(canEdit && node.ply > 0)) return undefined;
  // Editable = side that just moved (clock on current node). White to move → black just moved → editable index 1; black to move → white just moved → index 0.
  const editableIndex = canEdit && node.ply > 0 ? (isWhiteTurn ? 1 : 0) : -1;

  if (study && study.clockEdit && study.clockEdit.path !== path) {
    const val = study.clockEdit.value;
    if (isClearClockValue(val)) {
      if (study.makeChange('setClock', { ch: study.vm.chapterId, path: study.clockEdit.path, clear: true })) {
        ctrl.tree.setClockAt(undefined, study.clockEdit.path);
      }
    } else {
      const parsed = parseTimeToCentis(val);
      if (parsed !== undefined && parsed >= 0) {
        if (
          study.makeChange('setClock', { ch: study.vm.chapterId, path: study.clockEdit.path, centis: parsed })
        ) {
          ctrl.tree.setClockAt(parsed, study.clockEdit.path);
        }
      }
    }
    study.closeClockEdit();
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
    const editable = editableIndex === index;
    const isEditing = !!clockEdit && clockEdit.slot === slotForIndex(index);
    return {
      centis: centis[index],
      // Active (blue) = side to move
      active: node.ply > 0 && index === (isWhiteTurn ? 0 : 1),
      cls,
      showTenths,
      pause,
      empty: empty && !isEditing,
      editable: editable, // true when this slot is editable (empty or has value)
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
      onEditInput: isEditing ? v => study.setClockEditValue(v) : undefined,
      onEditKeydown: isEditing
        ? (e: KeyboardEvent) => {
            if (e.key === 'Enter') {
              const val = clockEdit.value;
              if (isClearClockValue(val)) {
                if (study.makeChange('setClock', { ch: study.vm.chapterId, path, clear: true })) {
                  ctrl.tree.setClockAt(undefined, path);
                  study.closeClockEdit();
                } else {
                  alert(i18n.study.turnOnRecToSaveClockTimes);
                }
              } else {
                const parsed = parseTimeToCentis(val);
                if (parsed !== undefined && parsed >= 0) {
                  const payload = { ch: study.vm.chapterId, path, centis: parsed };
                  if (study.makeChange('setClock', payload)) {
                    ctrl.tree.setClockAt(parsed, path);
                    study.closeClockEdit();
                  } else {
                    alert(i18n.study.turnOnRecToSaveClockTimes);
                  }
                } else if (val.trim() !== '') {
                  study.setClockEditError(true);
                }
              }
            } else if (e.key === 'Escape') {
              study.closeClockEdit();
            }
          }
        : undefined,
      onEditBlur: isEditing
        ? () => {
            const val = clockEdit.value;
            if (isClearClockValue(val)) {
              if (study.makeChange('setClock', { ch: study.vm.chapterId, path, clear: true })) {
                ctrl.tree.setClockAt(undefined, path);
              }
            } else {
              const parsed = parseTimeToCentis(val);
              if (parsed !== undefined && parsed >= 0) {
                if (study.makeChange('setClock', { ch: study.vm.chapterId, path, centis: parsed })) {
                  ctrl.tree.setClockAt(parsed, path);
                }
              }
            }
            study.closeClockEdit();
          }
        : undefined,
      hasError: isEditing && !!clockEdit.error,
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
    return [
      h('input.analyse__clock-input', {
        class: { 'analyse__clock-input--error': !!opts.hasError },
        attrs: {
          type: 'text',
          value: opts.editValue,
          placeholder: i18n.study.clockTimePlaceholder,
          title: i18n.study.clockTimeFormat,
          'data-cy': 'clock-input',
        },
        hook: {
          insert: vnode => {
            (vnode.elm as HTMLInputElement).focus();
            (vnode.elm as HTMLInputElement).select();
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
