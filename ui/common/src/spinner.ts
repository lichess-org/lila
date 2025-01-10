import { type VNode, h } from 'snabbdom';

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
          }),
        ),
      ]),
    ],
  );
}

export const spinnerHtml: string = `<div class="spinner"><svg viewBox="-2.5 -2.5 45 55" xmlns="http://www.w3.org/2000/svg">
  <path d="M 20 0 L 33 4 L 40 50 L 0 50 L 7 4 Z"
    style="fill:none;stroke-width:2.5;stroke-opacity:1;" />
</svg></div>`;
