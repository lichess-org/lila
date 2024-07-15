import { VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { prop } from 'common';
import { snabDialog } from 'common/dialog';
import flairPickerLoader from 'bits/flairPicker';
import { bindSubmit, bindNonPassive, onInsert, looseH as h } from 'common/snabbdom';
import { emptyRedButton } from '../view/util';
import { StudyData } from './interfaces';
import { Redraw } from '../interfaces';
import RelayCtrl from './relay/relayCtrl';

export interface FormData {
  name: string;
  flair?: string;
  visibility: string;
  computer: string;
  explorer: string;
  cloneable: string;
  shareable: string;
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

export class StudyForm {
  initAt = Date.now();
  open = prop(false);

  constructor(
    private readonly doSave: (data: FormData, isNew: boolean) => void,
    readonly getData: () => StudyData,
    readonly trans: Trans,
    readonly redraw: Redraw,
    readonly relay?: RelayCtrl,
  ) {}

  isNew = (): boolean => {
    const d = this.getData();
    return d.from === 'scratch' && !!d.isNew && Date.now() - this.initAt < 9000;
  };

  openIfNew = () => {
    if (this.isNew()) this.open(true);
  };
  save = (data: FormData, isNew: boolean) => {
    this.doSave(data, isNew);
    this.open(false);
  };
}

const select = (s: Select): VNode =>
  h('div.form-group.form-half', [
    h('label.form-label', { attrs: { for: 'study-' + s.key } }, s.name),
    h(
      `select#study-${s.key}.form-control`,
      s.choices.map(o => h('option', { attrs: { value: o[0], selected: s.selected === o[0] } }, o[1])),
    ),
  ]);

export function view(ctrl: StudyForm): VNode {
  const data = ctrl.getData();
  const isNew = ctrl.isNew();
  const updateName = (vnode: VNode, isUpdate: boolean) => {
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
  return snabDialog({
    class: 'study-edit',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    vnodes: [
      h('h2', ctrl.trans.noarg(ctrl.relay ? 'editRoundStudy' : isNew ? 'createStudy' : 'editStudy')),
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
                flair: getVal('flair'),
                visibility: getVal('visibility'),
                computer: getVal('computer'),
                explorer: getVal('explorer'),
                cloneable: getVal('cloneable'),
                shareable: getVal('shareable'),
                chat: getVal('chat'),
                sticky: getVal('sticky') as 'true' | 'false',
                description: getVal('description') as 'true' | 'false',
              },
              isNew,
            );
          }, ctrl.redraw),
        },
        [
          h('div.form-split.flair-and-name' + (ctrl.relay ? '.none' : ''), [
            h('div.form-group', [
              h('label.form-label', 'Flair'),
              h(
                'details.form-control.emoji-details',
                {
                  hook: onInsert(el => flairPickerLoader(el)),
                },
                [
                  h('summary.button.button-metal.button-no-upper', [
                    h('span.flair-container', [
                      h('img.uflair', {
                        attrs: { src: data.flair ? site.asset.flairSrc(data.flair) : '' },
                      }),
                    ]),
                  ]),
                  h('input#study-flair', {
                    attrs: { type: 'hidden', name: 'flair', value: data.flair || '' },
                  }),
                  h('div.flair-picker', {
                    attrs: { 'data-except-emojis': 'activity.lichess' },
                  }),
                ],
              ),
              data.flair && h(removeEmojiButton, 'Delete'),
            ]),
            h('div.form-group', [
              h('label.form-label', { attrs: { for: 'study-name' } }, ctrl.trans.noarg('name')),
              h('input#study-name.form-control', {
                attrs: { minlength: 3, maxlength: 100 },
                hook: {
                  insert: vnode => updateName(vnode, false),
                  postpatch: (_, vnode) => updateName(vnode, true),
                },
              }),
            ]),
          ]),
          h('div.form-split', [
            select({
              key: 'visibility',
              name: ctrl.trans.noarg('visibility'),
              choices: [
                ['public', ctrl.trans.noarg('public')],
                ['unlisted', ctrl.trans.noarg('unlisted')],
                ['private', ctrl.trans.noarg('inviteOnly')],
              ],
              selected: data.visibility,
            }),
            select({
              key: 'chat',
              name: ctrl.trans.noarg('chat'),
              choices: userSelectionChoices,
              selected: data.settings.chat,
            }),
          ]),
          h('div.form-split', [
            select({
              key: 'computer',
              name: ctrl.trans.noarg('computerAnalysis'),
              choices: userSelectionChoices.map(c => [c[0], ctrl.trans.noarg(c[1])]),
              selected: data.settings.computer,
            }),
            select({
              key: 'explorer',
              name: ctrl.trans.noarg('openingExplorerAndTablebase'),
              choices: userSelectionChoices,
              selected: data.settings.explorer,
            }),
          ]),
          h('div.form-split', [
            select({
              key: 'cloneable',
              name: ctrl.trans.noarg('allowCloning'),
              choices: userSelectionChoices,
              selected: data.settings.cloneable,
            }),
            select({
              key: 'shareable',
              name: ctrl.trans.noarg('shareAndExport'),
              choices: userSelectionChoices,
              selected: data.settings.shareable,
            }),
          ]),
          h('div.form-split', [
            select({
              key: 'sticky',
              name: ctrl.trans.noarg('enableSync'),
              choices: [
                ['true', ctrl.trans.noarg('yesKeepEveryoneOnTheSamePosition')],
                ['false', ctrl.trans.noarg('noLetPeopleBrowseFreely')],
              ],
              selected: '' + data.settings.sticky,
            }),
            select({
              key: 'description',
              name: ctrl.trans.noarg('pinnedStudyComment'),
              choices: [
                ['false', ctrl.trans.noarg('noPinnedComment')],
                ['true', ctrl.trans.noarg('rightUnderTheBoard')],
              ],
              selected: '' + data.settings.description,
            }),
          ]),
          ctrl.relay &&
            h('div.form-actions-secondary', [
              h(
                'a.text',
                {
                  attrs: {
                    'data-icon': licon.RadioTower,
                    href: `/broadcast/${ctrl.relay.data.tour.id}/edit`,
                  },
                },
                'Tournament settings',
              ),
              h(
                'a.text',
                { attrs: { 'data-icon': licon.RadioTower, href: `/broadcast/round/${data.id}/edit` } },
                'Round settings',
              ),
            ]),
          h('div.form-actions', [
            h('div', { attrs: { style: 'display: flex' } }, [
              h(
                'form',
                {
                  attrs: { action: '/study/' + data.id + '/delete', method: 'post' },
                  hook: bindNonPassive(
                    'submit',
                    _ =>
                      isNew ||
                      prompt(ctrl.trans('confirmDeleteStudy', data.name))?.trim() === data.name.trim(),
                  ),
                },
                [h(emptyRedButton, ctrl.trans.noarg(isNew ? 'cancel' : 'deleteStudy'))],
              ),
              !isNew &&
                h(
                  'form',
                  {
                    attrs: { action: '/study/' + data.id + '/clear-chat', method: 'post' },
                    hook: bindNonPassive('submit', _ =>
                      confirm(ctrl.trans.noarg('deleteTheStudyChatHistory')),
                    ),
                  },
                  [h(emptyRedButton, ctrl.trans.noarg('clearChat'))],
                ),
            ]),
            h('button.button', { attrs: { type: 'submit' } }, ctrl.trans.noarg(isNew ? 'start' : 'save')),
          ]),
        ],
      ),
    ],
  });
}

const removeEmojiButton = emptyRedButton + '.text.emoji-remove';
