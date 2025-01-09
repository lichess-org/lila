import { h, VNode, VNodes } from 'snabbdom';
import TournamentController from '../ctrl';
import header from './header';
import { colors } from 'shogiground/constants';
import { bind, MaybeVNode } from 'common/snabbdom';
import { opposite } from 'shogiground/util';
import { NewArrangement } from '../interfaces';
import { flatpickrInput } from './flatpickrs';
import { backControl, utcControl } from './controls';
import { arrangementHasUser } from './util';
import { arrangementThumbnail } from './arrangement-thumbnail';
import { colorName } from 'shogi/color-name';
import { i18n } from 'i18n';

export function organizedArrangementView(ctrl: TournamentController): VNodes {
  return [
    header(ctrl),
    backControl(ctrl, () => {
      ctrl.newArrangement = undefined;
    }, [utcControl(ctrl)]),
    organizerArrangement(ctrl),
  ];
}

function organizerArrangement(ctrl: TournamentController): VNode {
  return h('div.oganized-arrangement', [organizerArrangementForm(ctrl)]);
}

const organizerArrangementForm = (ctrl: TournamentController): MaybeVNode => {
  const state = ctrl.newArrangement;
  if (!state) return;
  const points = state.points;

  const canSubmit = state.user1?.id && state.user2?.id,
    isNew = !state.id;

  const user1Id = state.user1?.id,
    user2Id = state.user2?.id,
    gamesBetweenUsers =
      user1Id && user2Id
        ? ctrl.data.standing.arrangements.filter(
            a => arrangementHasUser(a, user1Id) && arrangementHasUser(a, user2Id)
          )
        : [];

  const updateState = <K extends keyof NewArrangement>(
    key: K,
    value: NewArrangement[K],
    redraw = false
  ) => {
    state[key] = value;
    if (redraw) ctrl.redraw();
  };
  const updateStateWithValidity = <K extends keyof NewArrangement>(
    key: K,
    value: NewArrangement[K],
    elm: HTMLInputElement,
    redraw = false
  ) => {
    if (elm.checkValidity()) updateState(key, value);
    else elm.value = state.allowGameBefore?.toString() || '';

    if (redraw) ctrl.redraw();
  };
  const updatePoints = (key: 'w' | 'd' | 'l', elm: HTMLInputElement) => {
    if (elm.checkValidity()) state.points[key] = parseInt(elm.value) || 0;
    else elm.value = state.points[key].toString() || '';
  };

  const handleSubmit = () => {
    const arrangement = {
      users: state.user1?.id + ';' + state.user2?.id,
      name: state.name || undefined,
      color: state.color ? state.color === 'sente' : undefined,
      points: state.points ? `${state.points.l};${state.points.d};${state.points.w}` : undefined,
      allowGameBefore: state.allowGameBefore ? state.allowGameBefore * 60 * 1000 : undefined,
      scheduledAt: state.scheduledAt ? new Date(state.scheduledAt).getTime() : undefined,
    };
    ctrl.newArrangementSettings({
      points: state.points,
      scheduledAt: state.scheduledAt,
      allowGameBefore: state.allowGameBefore,
    });
    ctrl.socket.send('arrangement-organizer', arrangement);
    ctrl.newArrangement = undefined;
  };

  return h('div.organizer-arrangement', [
    h('div.field-wrap.name', [
      h('label', 'Match name '),
      h('input', {
        attrs: { type: 'text', value: state.name || '', maxlength: 30 },
        on: {
          input: (e: Event) => {
            const elm = e.target as HTMLInputElement;
            updateStateWithValidity('name', elm.value, elm);
          },
        },
      }),
    ]),

    h('div.field-wrap.players', [
      h('label', 'Players*'),
      h('div.sides.search-wrap', [
        h(
          'select',
          {
            on: {
              change: (e: Event) => {
                updateState('user1', { id: (e.target as HTMLInputElement).value }, true);
              },
            },
          },
          playerOptions(ctrl, state.user1?.id, state.user2?.id)
        ),
        h('span', 'vs'),
        h(
          'select',
          {
            on: {
              change: (e: Event) => {
                updateState('user2', { id: (e.target as HTMLInputElement).value }, true);
              },
            },
          },
          playerOptions(ctrl, state.user2?.id, state.user1?.id)
        ),
      ]),
    ]),
    h('div.field-wrap.sides.color', [
      h('label', 'Color'),
      h('div.sides.color-wrap', [
        h(
          'select',
          {
            key: state.color,
            attrs: { value: state.color || '' },
            on: {
              change: (e: Event) =>
                updateState(
                  'color',
                  (e.target as HTMLInputElement).value as Color | undefined,
                  true
                ),
            },
          },
          colorOptions(state.color, false)
        ),
        h(
          'select',
          {
            key: state.color,
            attrs: { value: state.color ? opposite(state.color) : '' },
            on: {
              change: (e: Event) =>
                updateState(
                  'color',
                  (e.target as HTMLInputElement).value as Color | undefined,
                  true
                ),
            },
          },
          colorOptions(state.color, true)
        ),
      ]),
    ]),
    h('div.field-wrap.points', [
      h('label', 'Points (Win/Draw/Loss)'),
      h('div', [
        h('input', {
          attrs: { type: 'text', inputmode: 'numberic', pattern: '[0-9]*', value: points.w },
          on: { input: (e: Event) => updatePoints('w', e.target as HTMLInputElement) },
        }),
        h('input', {
          attrs: { type: 'text', inputmode: 'numberic', pattern: '[0-9]*', value: points.d },
          on: { input: (e: Event) => updatePoints('d', e.target as HTMLInputElement) },
        }),
        h('input', {
          attrs: { type: 'text', inputmode: 'numberic', pattern: '[0-9]*', value: points.l },
          on: { input: (e: Event) => updatePoints('l', e.target as HTMLInputElement) },
        }),
      ]),
    ]),
    h('div.field-wrap.scheduled-at', [
      h('label', 'Scheduled At'),
      flatpickrInput(
        false,
        state.scheduledAt,
        d => {
          state.scheduledAt = d.getTime();
          ctrl.redraw();
        },
        () => ctrl.utc()
      ),
    ]),
    h(
      'div.field-wrap.game-before',
      {
        class: {
          disabled: !state.scheduledAt,
        },
      },
      [
        h('label', 'Limit game start to before scheduled (minutes)'),
        h('input', {
          attrs: {
            type: 'text',
            inputmode: 'numberic',
            pattern: '[0-9]*',
            disabled: !state.scheduledAt,
            value: (!!state.scheduledAt && state.allowGameBefore) || '',
          },
          on: {
            input: (e: Event) => {
              updateStateWithValidity(
                'allowGameBefore',
                parseInt((e.target as HTMLInputElement).value),
                e.target as HTMLInputElement
              );
            },
          },
        }),
      ]
    ),
    h(
      'button.button',
      { hook: bind('click', () => handleSubmit()), class: { disabled: !canSubmit } },
      isNew ? 'Create new game arrangement' : 'Update arrangement'
    ),
    gamesBetweenUsers.length
      ? h('div.games-users-wrap', [
          h('h4', 'Existing games of players'),
          h(
            'div.games-users',
            h(
              'div',
              gamesBetweenUsers.map(g => arrangementThumbnail(ctrl, g))
            )
          ),
        ])
      : null,
  ]);
};

function colorOptions(stateColor: Color | undefined, flip: boolean) {
  return [
    h('option', { attrs: { value: '', selected: !stateColor } }, i18n('randomColor')),
    ...colors.map(color => {
      const realColor = flip ? opposite(color) : color;
      return h(
        'option',
        { attrs: { value: realColor, selected: stateColor === realColor } },
        colorName(color, false)
      );
    }),
  ];
}

function playerOptions(
  ctrl: TournamentController,
  selectedUserId: string | undefined,
  otherUserId: string | undefined
) {
  return [
    h('option', { attrs: { value: '', disabled: true, hidden: true, selected: !selectedUserId } }),
    ...ctrl.data.standing.players.map(player => {
      return h(
        'option',
        {
          attrs: {
            value: player.id,
            selected: player.id === selectedUserId,
            disabled: player.id === otherUserId,
          },
        },
        player.name
      );
    }),
  ];
}
