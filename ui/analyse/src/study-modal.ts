import { modal } from 'common/modal';
import { bind, onInsert } from 'common/snabbdom';
// import { debounce } from 'common/timings';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type AnalyseCtrl from './ctrl';

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
        i18n('study:createStudy'),
      ),
    ],
  );
}

// todo
function postGameStudyForm(ctrl: AnalyseCtrl): VNode {
  return h(
    'form',
    {
      hook: onInsert(el => {
        el.addEventListener('submit', (e: any) => {
          e.preventDefault();
          const formData = $(e.target).serialize();
          console.log('FORMDMAD1:', formData);
          // todo
          console.log('FORMDMAD2:', window.lishogi.xhr.formToXhr(e.target));
          // debounce(
          //   () => {
          //     window.lishogi.xhr.formToXhr(e)
          //       .then(res => {
          //         if (res.redirect) {
          //           window.lishogi.properReload = true;
          //           window.location.href = res.redirect;
          //         }
          //       })
          //       .catch(res => {
          //         alert(`${res.statusText} - ${res.error}`);
          //       });
          //   },
          //   1000,
          //   true
          // )();
        });
      }),
    },
    [
      h('input', {
        attrs: { type: 'hidden', name: 'gameId', value: ctrl.data.game.id },
      }),
      h('div', [
        h('label', i18n('studyWith')),
        h('input.user-invite', {
          hook: onInsert<HTMLInputElement>(el => {
            window.lishogi.userAutocomplete($(el), {
              tag: 'span',
              focus: true,
            });
          }),
          attrs: {
            name: 'invited',
            placeholder: `${i18n('study:searchByUsername')} (${i18n('optional').toLowerCase()})`,
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
        i18n('study:createStudy'),
      ),
    ],
  );
}

export function studyAdvancedButton(ctrl: AnalyseCtrl, menuIsOpen: boolean): VNode | null {
  return h('button.fbt', {
    attrs: {
      'data-icon': '4',
      disabled: !document.body.dataset.user,
      title: i18n('toStudy'),
      hidden: menuIsOpen,
    },
    hook: bind('click', _ => {
      ctrl.studyModal(true);
      ctrl.redraw();
    }),
  });
}

export function studyModal(ctrl: AnalyseCtrl): VNode {
  const d = ctrl.data;
  return modal({
    class: 'study__invite',
    onClose() {
      ctrl.studyModal(false);
      ctrl.redraw();
    },
    content: [
      h('div', [
        h('div.study-option', [
          h('div.study-title', i18n('postGameStudy')),
          h('div.desc', i18n('postGameStudyExplanation')),
          postGameStudyForm(ctrl),
          h(
            'a.text',
            { attrs: { 'data-icon': '', href: `/study/post-game-study/${d.game.id}/hot` } },
            i18n('postGameStudiesOfGame'),
          ),
        ]),
        h('div.study-option', [
          h('div.study-title', i18n('standardStudy')),
          standardStudyForm(ctrl),
        ]),
      ]),
    ],
  });
}
