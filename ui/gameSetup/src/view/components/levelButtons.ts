import { h } from 'snabbdom';
import { SetupCtrl } from '../../ctrl';
import { option } from './option';

export const levelButtons = (ctrl: SetupCtrl) => {
  return lichess.blindMode
    ? [
        h('label', { attrs: { for: 'sf_level' } }, ctrl.root.trans('strength')),
        h(
          'select#sf_level',
          {
            on: { change: (e: Event) => ctrl.aiLevel(parseInt((e.target as HTMLSelectElement).value)) },
          },
          '12345678'.split('').map(key => option({ key, name: key }, ctrl.aiLevel().toString())),
        ),
      ]
    : [
        h('br'),
        ctrl.root.trans('strength'),
        h('div.level.buttons', [
          h(
            'div.config_level',
            h(
              'group.radio',
              [1, 2, 3, 4, 5, 6, 7, 8].map(level =>
                h('div', [
                  h(`input#sf_level_${level}`, {
                    attrs: {
                      name: 'level',
                      type: 'radio',
                      value: level,
                      checked: level === ctrl.aiLevel(),
                    },
                    on: {
                      change: (e: Event) => ctrl.aiLevel(parseInt((e.target as HTMLInputElement).value)),
                    },
                  }),
                  h('label', { attrs: { for: `sf_level_${level}` } }, level),
                ]),
              ),
            ),
          ),
          h(
            'div.ai_info',
            h(
              `div.sf_level_${ctrl.aiLevel()}`,
              ctrl.root.trans('aiNameLevelAiLevel', 'Fairy-Stockfish 14', ctrl.aiLevel()),
            ),
          ),
        ]),
      ];
};
