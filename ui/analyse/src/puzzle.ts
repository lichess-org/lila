import { h, VNode } from 'snabbdom';
import AnalyseCtrl from './ctrl';

export function render(ctrl: AnalyseCtrl): VNode | undefined {
  const puzzle = ctrl.data.puzzle;
  return (
    puzzle &&
    h(
      'div.analyse__puzzle',
      h(
        'a.button.text',
        {
          attrs: {
            'data-icon': '',
            href: `/training/${puzzle.key}/${ctrl.bottomColor()}`,
          },
        },
        puzzle.name
      )
    )
  );
}
