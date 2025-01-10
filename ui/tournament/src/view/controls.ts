import { type MaybeVNode, bind } from 'common/snabbdom';
import { i18n } from 'i18n';
import { type VNode, type VNodes, h } from 'snabbdom';
import type TournamentController from '../ctrl';
import type { PageData } from '../interfaces';
import * as pagination from '../pagination';
import * as buttons from './button';
import { organizeArrangementButton } from './organized';

export function arenaControls(ctrl: TournamentController, pag: PageData): VNode {
  return h('div.tour__controls', [
    h('div.pager', pagination.renderPager(ctrl, pag)),
    h('div.right', [buttons.managePlayers(ctrl), buttons.joinWithdraw(ctrl)]),
  ]);
}

export function robinControls(ctrl: TournamentController): VNode {
  return h(
    'div.tour__controls',
    {
      hook: {
        insert: () => {
          robinArrowControls(ctrl);
        },
      },
    },
    [
      h('div.pager', [
        controlButton(i18n('study:first'), 'W', 'first'),
        controlButton(i18n('study:previous'), 'Y', 'prev'),
        controlButton(i18n('study:next'), 'X', 'next'),
        controlButton(i18n('study:last'), 'V', 'last'),
      ]),
      h('div.right', [buttons.managePlayers(ctrl), buttons.joinWithdraw(ctrl)]),
    ],
  );
}

export function organizedControls(ctrl: TournamentController, pag: PageData): VNode {
  return h('div.tour__controls', [
    h('div.pager', pagination.renderPager(ctrl, pag)),
    h('div.right', [
      organizeArrangementButton(ctrl),
      buttons.managePlayers(ctrl),
      buttons.joinWithdraw(ctrl),
    ]),
  ]);
}

export function backControl(ctrl: TournamentController, f: () => void, extra: VNodes = []): VNode {
  console.log('extra:', extra.length);

  return h('div.tour__controls.back', [
    h(
      'div.pager',
      { hook: bind('click', () => f(), ctrl.redraw) },
      h(
        'button.fbt.is.text.' + 'back',
        {
          attrs: {
            'data-icon': 'I',
            title: i18n('back'),
          },
        },
        i18n('back'),
      ),
    ),
    ...extra,
  ]);
}

export function utcControl(ctrl: TournamentController): VNode {
  return h('div.switch', [
    h('label.label', { attrs: { for: 'f-UTC' } }, 'UTC'),
    h('input.cmn-toggle', {
      attrs: {
        id: 'f-UTC',
        type: 'checkbox',
        checked: ctrl.utc(),
      },
      hook: bind(
        'change',
        e => {
          const val = (e.target as HTMLInputElement).checked;
          ctrl.utc(val);
        },
        ctrl.redraw,
      ),
    }),
    h('label', { attrs: { for: 'f-UTC' } }),
  ]);
}

function controlButton(text: string, icon: string, cls: string, el: MaybeVNode = undefined): VNode {
  return h(
    'button.fbt.is.' + cls,
    {
      attrs: {
        'data-icon': icon,
        title: text,
      },
    },
    el,
  );
}

function robinArrowControls(ctrl: TournamentController) {
  const container = document.querySelector('.r-table-wrap-arrs') as HTMLElement,
    table = container.querySelector('table') as HTMLElement,
    controls = document.querySelector('.tour__controls') as HTMLElement,
    firstArrow = controls.querySelector('button.first') as HTMLElement,
    prevArrow = controls.querySelector('button.prev') as HTMLElement,
    nextArrow = controls.querySelector('button.next') as HTMLElement,
    lastArrow = controls.querySelector('button.last') as HTMLElement;

  function updateArrowState() {
    const canScrollLeft = container.scrollLeft > 0,
      canScrollRight =
        Math.round(container.scrollLeft) < container.scrollWidth - container.clientWidth - 1;

    firstArrow.classList.toggle('disabled', !canScrollLeft);
    prevArrow.classList.toggle('disabled', !canScrollLeft);

    nextArrow.classList.toggle('disabled', !canScrollRight);
    lastArrow.classList.toggle('disabled', !canScrollRight);
  }

  function calculateColumnWidth() {
    const tableWidth = table.offsetWidth;
    return tableWidth / ctrl.data.standing.players.length;
  }

  function scrollLeft(max = false) {
    const columnWidth = calculateColumnWidth();
    const scrollDistance = max
      ? -container.scrollLeft
      : -((Math.floor(container.clientWidth / columnWidth) - 1) * columnWidth);

    container.scrollBy({
      left: scrollDistance,
      behavior: 'smooth',
    });
  }

  function scrollRight(max = false) {
    const columnWidth = calculateColumnWidth();
    const maxScrollRight = container.scrollWidth - container.clientWidth;
    const scrollDistance = max
      ? maxScrollRight - container.scrollLeft
      : (Math.floor(container.clientWidth / columnWidth) - 1) * columnWidth;

    container.scrollBy({
      left: scrollDistance,
      behavior: 'smooth',
    });
  }

  firstArrow.addEventListener('click', () => scrollLeft(true));
  prevArrow.addEventListener('click', () => scrollLeft(false));
  nextArrow.addEventListener('click', () => scrollRight(false));
  lastArrow.addEventListener('click', () => scrollRight(true));

  container.addEventListener('scroll', updateArrowState);
  window.addEventListener('resize', updateArrowState);

  updateArrowState();
}
