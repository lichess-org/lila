import { bind } from 'common/snabbdom';
import { h, VNode } from 'snabbdom';
import LobbyController from '../../ctrl';
import { i18n } from 'i18n';

function initialize(ctrl: LobbyController, el: HTMLElement) {
  const f = ctrl.filter.data?.form,
    isSeek: boolean = ctrl.tab === 'seeks',
    $div = $(el);

  if (f)
    Object.keys(f).forEach(k => {
      const input = $div.find(`input[name="${k}"]`)[0] as HTMLInputElement;
      if (input.type == 'checkbox') input.checked = true;
      else input.value = f[k];
    });
  else $div.find('input').prop('checked', true);

  $('.f-real_time').toggleClass('none', isSeek);
  $('.f-seeks').toggleClass('none', !isSeek);

  const save = () => ctrl.filter.save($div.find('form')[0] as HTMLFormElement);

  $div.find('input').change(save);
  $div.find('button.reset').click(function () {
    ctrl.filter.save(null);
    ctrl.filter.open = false;
    ctrl.redraw();
  });
  $div.find('button.apply').click(function () {
    ctrl.filter.open = false;
    ctrl.redraw();
  });
}

export function toggle(ctrl: LobbyController, nbFiltered: number): VNode {
  const filter = ctrl.filter,
    hasFiltered = nbFiltered > 0;
  return h(
    'i.toggle.toggle-filter',
    {
      class: { gamesFiltered: hasFiltered, active: filter.open },
      hook: bind('mousedown', filter.toggle, ctrl.redraw),
      attrs: {
        'data-icon': filter.open ? 'L' : '%',
        title: i18n('filterGames'),
      },
    },
    hasFiltered && !filter.open ? h('i.unread', nbFiltered < 10 ? nbFiltered : '9+') : undefined,
  );
}

export interface FilterNode extends HTMLElement {
  filterLoaded?: boolean;
}

export function render(ctrl: LobbyController): VNode {
  return h('div.hook__filters', {
    hook: {
      insert(vnode) {
        const el = vnode.elm as FilterNode;
        if (el.filterLoaded) return;
        window.lishogi.xhr
          .text('GET', '/setup/filter', undefined, { cache: 'default' })
          .then(html => {
            el.innerHTML = html;
            el.filterLoaded = true;
            initialize(ctrl, el);
          });
      },
    },
  });
}
