import { h, type VNode } from 'snabbdom';

import type StudyClockEdit from './studyClockEdit';

export function renderClockEditInput(
  session: StudyClockEdit,
  handlers: { onInput: (v: string) => void; onKeydown: (e: KeyboardEvent) => void; onBlur: () => void },
): VNode {
  let shakeClass: string | undefined;
  if (session.error && session.shakeTrigger > 0) {
    // Alternate classes so the shake animation re-triggers on repeated invalid submits.
    shakeClass =
      session.shakeTrigger % 2 === 1 ? 'analyse__clock-input--shake-odd' : 'analyse__clock-input--shake-even';
  }
  const classList: Record<string, boolean> = { 'analyse__clock-input--error': session.error };
  if (shakeClass) classList[shakeClass] = true;

  return h('input.analyse__clock-input', {
    class: classList,
    attrs: {
      type: 'text',
      value: session.value,
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
      input: (e: Event) => handlers.onInput((e.target as HTMLInputElement).value),
      keydown: (e: KeyboardEvent) => handlers.onKeydown(e),
      blur: () => handlers.onBlur(),
    },
  });
}
