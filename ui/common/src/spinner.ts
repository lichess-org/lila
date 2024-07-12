import { h, VNode } from 'snabbdom';

export const spinnerHtml =
  '<div class="spinner" aria-label="loading"><svg viewBox="-2 -2 54 54"><g mask="url(#mask)" fill="none"><path id="a" stroke-width="3.779" d="m21.78 12.64c-1.284 8.436 8.943 12.7 14.54 17.61 3 2.632 4.412 4.442 5.684 7.93"/><path id="b" stroke-width="4.157" d="m43.19 36.32c2.817-1.203 6.659-5.482 5.441-7.623-2.251-3.957-8.883-14.69-11.89-19.73-0.4217-0.7079-0.2431-1.835 0.5931-3.3 1.358-2.38 1.956-5.628 1.956-5.628"/><path id="c" stroke-width="4.535" d="m37.45 2.178s-3.946 0.6463-6.237 2.234c-0.5998 0.4156-2.696 0.7984-3.896 0.6388-17.64-2.345-29.61 14.08-25.23 27.34 4.377 13.26 22.54 25.36 39.74 8.666"/></g></svg></div>';

const pathAttrs = [
  {
    'stroke-width': 3.779,
    d: 'm21.78 12.64c-1.284 8.436 8.943 12.7 14.54 17.61 3 2.632 4.412 4.442 5.684 7.93',
  },
  {
    'stroke-width': 4.157,
    d: 'm43.19 36.32c2.817-1.203 6.659-5.482 5.441-7.623-2.251-3.957-8.883-14.69-11.89-19.73-0.4217-0.7079-0.2431-1.835 0.5931-3.3 1.358-2.38 1.956-5.628 1.956-5.628',
  },
  {
    'stroke-width': 4.535,
    d: 'm37.45 2.178s-3.946 0.6463-6.237 2.234c-0.5998 0.4156-2.696 0.7984-3.896 0.6388-17.64-2.345-29.61 14.08-25.23 27.34 4.377 13.26 22.54 25.36 39.74 8.666',
  },
];

export const spinnerVdom = (box = '-2 -2 54 54'): VNode =>
  h('div.spinner', { 'aria-label': 'loading' }, [
    h('svg', { attrs: { viewBox: box } }, [
      h(
        'g',
        { attrs: { mask: 'url(#mask)', fill: 'none' } },
        pathAttrs.map(attrs => h('path', { attrs })),
      ),
    ]),
  ]);

export const stockfishName = 'Stockfish 16.1';

export const chartSpinner = (): VNode =>
  h('div#acpl-chart-container-loader', [
    h('span', [stockfishName, h('br'), 'Server analysis']),
    spinnerVdom(),
  ]);
