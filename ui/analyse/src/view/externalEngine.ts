import { bind } from 'common/snabbdom';
import { h } from 'snabbdom';
import AnalyseCtrl from '../ctrl';

export function config(ctrl: AnalyseCtrl) {
  const engines = ctrl.data.externalEngines;
  if (!engines?.length || !ctrl.ceval?.possible || !ctrl.ceval.allowed()) return [];
  return [
    h('h2', ctrl.trans.noarg('engineManager')),
    h(
      'select.external__select.setting',
      {
        hook: bind('change', e => ctrl.getCeval().selectEngine((e.target as HTMLSelectElement).value)),
      },
      [
        h('option', { attrs: { value: 'lichess' } }, 'Lichess'),
        ...engines.map(engine =>
          h(
            'option',
            {
              attrs: {
                value: engine.id,
                selected: ctrl.getCeval().externalEngine?.id == engine.id,
              },
            },
            engine.name,
          ),
        ),
      ],
    ),
  ];
}
