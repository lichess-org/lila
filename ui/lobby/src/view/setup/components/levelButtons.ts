import { h } from 'snabbdom';
import LobbyController from '../../../ctrl';
import { option } from './option';

export const levelButtons = (ctrl: LobbyController) =>
  ctrl.opts.blindMode
    ? [
        h('label', { attrs: { for: 'sf_level' } }, ctrl.trans('strength')),
        h(
          'select#sf_level',
          { on: { change: (e: Event) => ctrl.setupCtrl.aiLevel(parseInt((e.target as HTMLSelectElement).value)) } },
          [1, 2, 3, 4, 5, 6, 7, 8].map(level =>
            option({ key: `${level}`, name: `${level}` }, ctrl.setupCtrl.aiLevel().toString())
          )
        ),
      ]
    : [
        h('br'),
        ctrl.trans('strength'),
        h('div.level.buttons', [
          h(
            'div.config_level',
            h(
              'group.radio',
              [1, 2, 3, 4, 5, 6, 7, 8].map(level =>
                h('div', [
                  h(`input#sf_level_${level}`, {
                    attrs: { type: 'radio', value: level, checked: level === ctrl.setupCtrl.aiLevel() },
                    on: {
                      change: (e: Event) => ctrl.setupCtrl.aiLevel(parseInt((e.target as HTMLInputElement).value)),
                    },
                  }),
                  h('label.required', { attrs: { for: `sf_level_${level}` } }, level),
                ])
              )
            )
          ),
          h(
            'div.ai_info',
            h(
              `div.sf_level_${ctrl.setupCtrl.aiLevel()}`,
              ctrl.trans('aiNameLevelAiLevel', 'Fairy-Stockfish 14', ctrl.setupCtrl.aiLevel())
            )
          ),
        ]),
      ];
