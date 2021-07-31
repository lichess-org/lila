import { h, VNode } from 'snabbdom';
import { snabModal } from 'common/modal';
import { prop, Prop } from 'common';
import { bindSubmit, bindNonPassive } from 'common/snabbdom';
import { emptyRedButton } from '../util';
import { StudyData } from './interfaces';
import { Redraw, MaybeVNodes } from '../interfaces';
import RelayCtrl from './relay/relayCtrl';

export interface StudyFormCtrl {
  open: Prop<boolean>;
  openIfNew(): void;
  save(data: FormData, isNew: boolean): void;
  getData(): StudyData;
  isNew(): boolean;
  trans: Trans;
  redraw: Redraw;
  relay?: RelayCtrl;
}

export interface FormData {
  name: string;
  visibility: string;
  computer: string;
  explorer: string;
  cloneable: string;
  chat: string;
  sticky: 'true' | 'false';
  description: 'true' | 'false';
}

interface Select {
  key: string;
  name: string;
  choices: Choice[];
  selected: string;
}
type Choice = [string, string];

function select(s: Select): MaybeVNodes {
  return [
    h(
      'label.form-label',
      {
        attrs: { for: 'study-' + s.key },
      },
      s.name
    ),
    h(
      `select#study-${s.key}.form-control`,
      s.choices.map(function (o) {
        return h(
          'option',
          {
            attrs: {
              value: o[0],
              selected: s.selected === o[0],
            },
          },
          o[1]
        );
      })
    ),
  ];
}

export function ctrl(
  save: (data: FormData, isNew: boolean) => void,
  getData: () => StudyData,
  trans: Trans,
  redraw: Redraw,
  relay?: RelayCtrl
): StudyFormCtrl {
  const initAt = Date.now();

  function isNew(): boolean {
    const d = getData();
    return d.from === 'scratch' && !!d.isNew && Date.now() - initAt < 9000;
  }

  const open = prop(false);

  return {
    open,
    openIfNew() {
      if (isNew()) open(true);
    },
    save(data: FormData, isNew: boolean) {
      save(data, isNew);
      open(false);
    },
    getData,
    isNew,
    trans,
    redraw,
    relay,
  };
}

export function view(ctrl: StudyFormCtrl): VNode {
  const data = ctrl.getData();
  const isNew = ctrl.isNew();
  const updateName = function (vnode: VNode, isUpdate: boolean) {
    const el = vnode.elm as HTMLInputElement;
    if (!isUpdate && !el.value) {
      el.value = data.name;
      if (isNew) el.select();
      el.focus();
    }
  };
  const userSelectionChoices: Choice[] = [
    ['nobody', ctrl.trans.noarg('nobody')],
    ['owner', ctrl.trans.noarg('onlyMe')],
    ['contributor', ctrl.trans.noarg('contributors')],
    ['member', ctrl.trans.noarg('members')],
    ['everyone', ctrl.trans.noarg('everyone')],
  ];
  return snabModal({
    class: 'study-edit',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', ctrl.trans.noarg(ctrl.relay ? 'configureLiveBroadcast' : isNew ? 'createStudy' : 'editStudy')),
      h(
        'form.form3',
        {
          hook: bindSubmit(e => {
            const getVal = (name: string): string => {
              const el = (e.target as HTMLElement).querySelector('#study-' + name) as HTMLInputElement;
              if (el) return el.value;
              else throw `Missing form input: ${name}`;
            };
            ctrl.save(
              {
                name: getVal('name'),
                visibility: getVal('visibility'),
                computer: getVal('computer'),
                explorer: getVal('explorer'),
                cloneable: getVal('cloneable'),
                chat: getVal('chat'),
                sticky: getVal('sticky') as 'true' | 'false',
                description: getVal('description') as 'true' | 'false',
              },
              isNew
            );
          }, ctrl.redraw),
        },
        [
          h('div.form-group' + (ctrl.relay ? '.none' : ''), [
            h('label.form-label', { attrs: { for: 'study-name' } }, ctrl.trans.noarg('name')),
            h('input#study-name.form-control', {
              attrs: {
                minlength: 3,
                maxlength: 100,
              },
              hook: {
                insert: vnode => updateName(vnode, false),
                postpatch: (_, vnode) => updateName(vnode, true),
              },
            }),
          ]),
          h('div.form-split', [
            h(
              'div.form-group.form-half',
              select({
                key: 'visibility',
                name: ctrl.trans.noarg('visibility'),
                choices: [
                  ['public', ctrl.trans.noarg('public')],
                  ['unlisted', ctrl.trans.noarg('unlisted')],
                  ['private', ctrl.trans.noarg('inviteOnly')],
                ],
                selected: data.visibility,
              })
            ),
            h(
              'div.form-group.form-half',
              select({
                key: 'cloneable',
                name: ctrl.trans.noarg('allowCloning'),
                choices: userSelectionChoices,
                selected: data.settings.cloneable,
              })
            ),
          ]),
          h('div.form-split', [
            h(
              'div.form-group.form-half',
              select({
                key: 'computer',
                name: ctrl.trans.noarg('computerAnalysis'),
                choices: userSelectionChoices.map(c => [c[0], ctrl.trans.noarg(c[1])]),
                selected: data.settings.computer,
              })
            ),
            h(
              'div.form-group.form-half',
              select({
                key: 'explorer',
                name: ctrl.trans.noarg('openingExplorer'),
                choices: userSelectionChoices,
                selected: data.settings.explorer,
              })
            ),
          ]),
          h('div.form-split', [
            h(
              'div.form-group.form-half',
              select({
                key: 'chat',
                name: ctrl.trans.noarg('chat'),
                choices: userSelectionChoices,
                selected: data.settings.chat,
              })
            ),
            h(
              'div.form-group.form-half',
              select({
                key: 'sticky',
                name: ctrl.trans.noarg('enableSync'),
                choices: [
                  ['true', ctrl.trans.noarg('yesKeepEveryoneOnTheSamePosition')],
                  ['false', ctrl.trans.noarg('noLetPeopleBrowseFreely')],
                ],
                selected: '' + data.settings.sticky,
              })
            ),
          ]),
          h(
            'div.form-group.form-half',
            select({
              key: 'description',
              name: ctrl.trans.noarg('pinnedStudyComment'),
              choices: [
                ['false', ctrl.trans.noarg('noPinnedComment')],
                ['true', ctrl.trans.noarg('rightUnderTheBoard')],
              ],
              selected: '' + data.settings.description,
            })
          ),
          h(`div.form-actions${ctrl.relay ? '' : '.single'}`, [
            ...(ctrl.relay
              ? [
                  h(
                    'a.text',
                    {
                      attrs: { 'data-icon': '', href: `/broadcast/${ctrl.relay.data.tour.id}/edit` },
                    },
                    'Tournament settings'
                  ),
                  h(
                    'a.text',
                    {
                      attrs: { 'data-icon': '', href: `/broadcast/round/${data.id}/edit` },
                    },
                    'Round settings'
                  ),
                ]
              : []),
            h(
              'button.button',
              {
                attrs: { type: 'submit' },
              },
              ctrl.trans.noarg(isNew ? 'start' : 'save')
            ),
          ]),
        ]
      ),
      h('div.destructive', [
        isNew
          ? null
          : h(
              'form',
              {
                attrs: {
                  action: '/study/' + data.id + '/clear-chat',
                  method: 'post',
                },
                hook: bindNonPassive('submit', _ => confirm(ctrl.trans.noarg('deleteTheStudyChatHistory'))),
              },
              [h(emptyRedButton, ctrl.trans.noarg('clearChat'))]
            ),
        h(
          'form',
          {
            attrs: {
              action: '/study/' + data.id + '/delete',
              method: 'post',
            },
            hook: bindNonPassive(
              'submit',
              _ => isNew || prompt(`${data.name}\n\n${ctrl.trans.noarg('confirmDeleteStudy')}`) === data.name
            ),
          },
          [h(emptyRedButton, ctrl.trans.noarg(isNew ? 'cancel' : 'deleteStudy'))]
        ),
      ]),
    ],
  });
}
