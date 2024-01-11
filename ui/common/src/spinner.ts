import { VNode, h } from 'snabbdom';

export default function (): VNode {
  return h(
    'div.spinner',
    {
      attrs: {
        'aria-label': 'loading',
      },
    },
    [
      h('svg', { attrs: { viewBox: '-2.5 -2.5 45 55' } }, [
        h(
          'g',
          h('path', {
            attrs: {
              d: 'M 20 0 L 33 4 L 40 50 L 0 50 L 7 4 Z',
              fill: 'none',
              'stroke-width': '2.5',
            },
          })
        ),
      ]),
    ]
  );
}
