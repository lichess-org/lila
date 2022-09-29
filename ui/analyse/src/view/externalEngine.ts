import { h } from 'snabbdom';
import AnalyseCtrl from '../ctrl';

export function config(ctrl: AnalyseCtrl) {
  const engines = ctrl.data.externalEngines;
  if (!engines?.length) return [];
  return [
    h('h2', 'External engines'),
    h('select.external__select.setting', [
      h('option', 'None'),
      ...engines.map(engine =>
        h(
          'option',
          {
            attrs: {
              value: engine.id,
            },
          },
          engine.name
        )
      ),
    ]),
  ];
}
