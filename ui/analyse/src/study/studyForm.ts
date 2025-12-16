import * as licon from 'lib/licon';
import { prop } from 'lib';
import { snabDialog, confirm, prompt } from 'lib/view';
import flairPickerLoader from 'bits/flairPicker';
import { type VNode, bindSubmit, bindNonPassive, onInsert, hl } from 'lib/view';
import { emptyRedButton } from '../view/util';
import type { StudyData } from './interfaces';
import type RelayCtrl from './relay/relayCtrl';

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
  visible: boolean;
}
type Choice = [string, string];

export class StudyForm {
  initAt = Date.now();
  open = prop(false);

  constructor(
    private readonly doSave: (data: FormData, isNew: boolean) => void,
    readonly getData: () => StudyData,
    readonly redraw: Redraw,
    readonly relay?: RelayCtrl,
  ) {}

  isNew = (): boolean => {
    const d = this.getData();
    return (
      !!d.isNew &&
      Date.now() - this.initAt < 9000 &&
      (d.from === 'scratch' || isObjectWithSomeProperty(d.from, ['study', 'game']))
    );
  };

  openIfNew = () => {
    if (this.isNew()) this.open(true);
  };
  save = (data: FormData, isNew: boolean) => {
    this.doSave(data, isNew);
    this.open(false);
  };
}

const isObjectWithSomeProperty = (o: unknown, keys: string[]): boolean =>
  typeof o === 'object' && o !== null && keys.some(k => k in o);

const select = (s: Select): VNode =>
  hl('div.form-group.form-half' + (s.visible ? '' : '.none'), [
    hl('label.form-label', { attrs: { for: 'study-' + s.key } }, s.name),
    hl(
      `select#study-${s.key}.form-control`,
      s.choices.map(o => hl('option', { attrs: { value: o[0], selected: s.selected === o[0] } }, o[1])),
    ),
  ]);

export function view(ctrl: StudyForm): VNode {
  const data = ctrl.getData();
  const isNew = ctrl.isNew();
  const isEditable = !ctrl.relay?.isOfficial();
  const updateName = (vnode: VNode, isUpdate: boolean) => {
    const el = vnode.elm as HTMLInputElement;
    if (!isUpdate && !el.value) {
      el.value = data.name;
      if (isNew) el.select();
      el.focus();
    }
  };
  const userSelectionChoices: Choice[] = [
    ['nobody', i18n.study.nobody],
    ['owner', i18n.study.onlyMe],
    ['contributor', i18n.study.contributors],
    ['member', i18n.study.members],
    ['everyone', i18n.study.everyone],
  ];
  const formFields = [
    hl('div.form-split.flair-and-name' + (ctrl.relay ? '.none' : ''), [
      hl('div.form-group', [
        hl('label.form-label', 'Flair'),
        hl(
          'div.form-control.emoji-details',
          {
            hook: onInsert(el => flairPickerLoader(el)),
          },
          [
            hl('div.emoji-popup-button', [
              hl(
                'select#study-flair.form-control',
                { attrs: { name: 'flair' } },
                data.flair && hl('option', { attrs: { value: data.flair, selected: true } }),
              ),
              hl('img', { attrs: { src: data.flair ? site.asset.flairSrc(data.flair) : '' } }),
            ]),
            hl(
              'div.flair-picker.none',
              data.admin || { attrs: { 'data-except-emojis': 'activity.lichess' } },
              hl('button.button.button-metal.emoji-remove', { attrs: { type: 'button' } }, 'clear'),
            ),
          ],
        ),
      ]),
      hl('div.form-group', [
        hl('label.form-label', { attrs: { for: 'study-name' } }, i18n.site.name),
        hl('input#study-name.form-control', {
          attrs: { minlength: 3, maxlength: 100 },
          hook: {
            insert: vnode => {
              updateName(vnode, false);
              const el = vnode.elm as HTMLInputElement;
              el.addEventListener('focus', () => el.select());
              // set initial modal focus
              setTimeout(() => el.focus());
            },
            postpatch: (_, vnode) => updateName(vnode, true),
          },
        }),
      ]),
    ]),
    hl('div.form-split', [
      select({
        key: 'visibility',
        name: i18n.study.visibility,
        choices: [
          ['public', i18n.study.public],
          ['unlisted', i18n.study.unlisted],
          ['private', i18n.study.inviteOnly],
        ],
        selected: data.visibility,
        visible: isEditable,
      }),
      select({
        key: 'chat',
        name: i18n.site.chat,
        choices: userSelectionChoices,
        selected: data.settings.chat,
        visible: isEditable,
      }),
    ]),
    hl('div.form-split', [
      select({
        key: 'computer',
        name: i18n.site.computerAnalysis,
        choices: userSelectionChoices,
        selected: data.settings.computer,
        visible: isEditable,
      }),
      select({
        key: 'explorer',
        name: i18n.site.openingExplorerAndTablebase,
        choices: userSelectionChoices,
        selected: data.settings.explorer,
        visible: isEditable,
      }),
    ]),
    hl('div.form-split', [
      select({
        key: 'cloneable',
        name: i18n.study.allowCloning,
        choices: userSelectionChoices,
        selected: data.settings.cloneable,
        visible: isEditable,
      }),
      select({
        key: 'shareable',
        name: i18n.study.shareAndExport,
        choices: userSelectionChoices,
        selected: data.settings.shareable,
        visible: isEditable,
      }),
    ]),
    hl('div.form-split', [
      select({
        key: 'sticky',
        name: i18n.study.enableSync,
        choices: [
          ['true', i18n.study.yesKeepEveryoneOnTheSamePosition],
          ['false', i18n.study.noLetPeopleBrowseFreely],
        ],
        selected: '' + data.settings.sticky,
        visible: isEditable,
      }),
      select({
        key: 'description',
        name: i18n.study.pinnedStudyComment,
        choices: [
          ['false', i18n.study.noPinnedComment],
          ['true', i18n.study.rightUnderTheBoard],
        ],
        selected: '' + data.settings.description,
        visible: true,
      }),
    ]),
  ];
  const relayLinks =
    ctrl.relay &&
    hl('div.form-actions-secondary', [
      hl(
        'a.text',
        {
          attrs: {
            'data-icon': licon.RadioTower,
            href: `/broadcast/${ctrl.relay.data.tour.id}/edit`,
          },
        },
        'Tournament settings',
      ),
      hl(
        'a.text',
        { attrs: { 'data-icon': licon.RadioTower, href: `/broadcast/round/${data.id}/edit` } },
        'Round settings',
      ),
    ]);
  const deleteForms = hl('div', { attrs: { style: 'display: flex' } }, [
    hl(
      'form',
      {
        attrs: { action: '/study/' + data.id + '/delete', method: 'post' },
        hook: bindNonPassive('submit', e => {
          if (isNew) return;

          e.preventDefault();
          prompt(i18n.study.confirmDeleteStudy(data.name)).then(userInput => {
            if (userInput?.trim() === data.name.trim()) (e.target as HTMLFormElement).submit();
          });
        }),
      },
      [hl(emptyRedButton, isNew ? i18n.site.cancel : i18n.study.deleteStudy)],
    ),
    !isNew &&
      hl(
        'form',
        {
          attrs: { action: '/study/' + data.id + '/clear-chat', method: 'post' },
          hook: bindNonPassive('submit', e => {
            e.preventDefault();
            confirm(i18n.study.deleteTheStudyChatHistory).then(yes => {
              if (yes) (e.target as HTMLFormElement).submit();
            });
          }),
        },
        [hl(emptyRedButton, i18n.study.clearChat)],
      ),
  ]);
  return snabDialog({
    class: 'study-edit',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    modal: true,
    noClickAway: true,
    vnodes: [
      hl(
        'h2',
        ctrl.relay ? i18n.broadcast.editRoundStudy : isNew ? i18n.study.createStudy : i18n.study.editStudy,
      ),
      hl(
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
          formFields,
          relayLinks,
          hl('div.form-actions', [
            deleteForms,
            hl('button.button', { attrs: { type: 'submit' } }, isNew ? i18n.study.start : i18n.study.save),
          ]),
        ],
      ),
    ],
  });
}
