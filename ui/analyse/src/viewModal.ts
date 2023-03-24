import { bind, onInsert } from 'common/snabbdom';
import { h, VNode } from 'snabbdom';
import AnalyseCtrl from './ctrl';

function standardStudyForm(ctrl: AnalyseCtrl): VNode {
  return h(
    'form',
    {
      attrs: {
        method: 'post',
        action: '/study/as',
      },
    },
    [
      h('input', {
        attrs: { type: 'hidden', name: 'gameId', value: ctrl.data.game.id },
      }),
      h('input', {
        attrs: { type: 'hidden', name: 'orientation', value: ctrl.shogiground.state.orientation },
      }),
      h(
        'button.button',
        {
          attrs: {
            type: 'submit',
          },
        },
        ctrl.trans.noarg('createStudy')
      ),
    ]
  );
}

function postGameStudyForm(ctrl: AnalyseCtrl): VNode {
  return h(
    'form',
    {
      hook: onInsert(el => {
        $(el).on('submit', e => {
          e.preventDefault();
          const formData = $(e.target).serialize();
          window.lishogi.debounce(
            () => {
              $.post('/study/post-game-study', formData)
                .done(res => {
                  if (res.redirect) {
                    window.lishogi.hasToReload = true;
                    window.location.href = res.redirect;
                  }
                })
                .fail(res => {
                  alert(`${res.statusText} - ${res.error}`);
                });
            },
            1000,
            true
          )();
        });
      }),
    },
    [
      h('input', {
        attrs: { type: 'hidden', name: 'gameId', value: ctrl.data.game.id },
      }),
      h('div', [
        h('label', ctrl.trans.noarg('studyWith')),
        h('input.user-autocomplete', {
          attrs: {
            name: 'invited',
            'data-tag': 'span',
            placeholder: ctrl.trans.noarg('searchByUsername') + ` (${ctrl.trans.noarg('optional').toLowerCase()})`,
          },
        }),
      ]),
      h('input', {
        attrs: { type: 'hidden', name: 'orientation', value: ctrl.shogiground.state.orientation },
      }),
      h(
        'button.button',
        {
          attrs: {
            type: 'submit',
          },
        },
        ctrl.trans.noarg('createStudy')
      ),
    ]
  );
}

export function studyAdvancedButton(ctrl: AnalyseCtrl, menuIsOpen: boolean): VNode | null {
  const d = ctrl.data;
  return h(
    'button.fbt',
    {
      attrs: {
        'data-icon': '4',
        disabled: !document.body.dataset.user,
        title: ctrl.trans.noarg('toStudy'),
        hidden: menuIsOpen,
      },
      hook: bind('click', _ => {
        $.modal($('.g_' + d.game.id), undefined, undefined, true);
      }),
    },
    [
      h('div.none.g_' + d.game.id, [
        h('div.study-option', [
          h('div.study-title', ctrl.trans.noarg('postGameStudy')),
          h('div.desc', ctrl.trans.noarg('postGameStudyExplanation')),
          postGameStudyForm(ctrl),
          h(
            'a.text',
            { attrs: { 'data-icon': '', href: `/study/post-game-study/${d.game.id}/hot` } },
            ctrl.trans.noarg('postGameStudiesOfGame')
          ),
        ]),
        h('div.study-option', [h('div.study-title', ctrl.trans.noarg('standardStudy')), standardStudyForm(ctrl)]),
      ]),
    ]
  );
}
