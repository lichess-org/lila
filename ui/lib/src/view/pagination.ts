import { h, type VNode } from 'snabbdom';
import * as licon from '../licon';
import { bind, onInsert, type MaybeVNodes } from './snabbdom';
import { userComplete, type UserCompleteOpts } from './userComplete';

export const maxPerPage = 10;

export interface PagerData<A> {
  from: number;
  to: number;
  currentPageResults: A[];
  nbResults: number;
  nbPages: number;
}

export interface PaginatedCtrl<A> {
  page: number;
  searching: boolean;
  redraw: () => void;
  data: { id: string; nbPlayers: number; me?: { rank: number } };
  pages: Record<number, A[]>;
  toggleFocusOnMe(): void;
  toggleSearch(): void;
  jumpToPageOf(name: string): void;
  jumpToRank(rank: number): void;
  userSetPage(page: number): void;
  userPrevPage(): void;
  userNextPage(): void;
  userLastPage(): void;
}

function navButton(
  text: string,
  icon: string,
  click: () => void,
  enable: boolean,
  redraw: () => void,
): VNode {
  return h('button.fbt.is', {
    attrs: { 'data-icon': icon, disabled: !enable, title: text },
    hook: bind('mousedown', click, redraw),
  });
}

function scrollToMeButton(ctrl: PaginatedCtrl<unknown>): VNode | undefined {
  return ctrl.data.me && myPage(ctrl) !== ctrl.page
    ? h('button.fbt', {
        attrs: { 'data-icon': licon.Target, title: 'Scroll to your player' },
        hook: bind('mousedown', ctrl.toggleFocusOnMe, ctrl.redraw),
      })
    : undefined;
}

export function renderPager<A>(ctrl: PaginatedCtrl<A>, searchButton: VNode, searchInput: VNode): MaybeVNodes {
  const pag = pagerData(ctrl);
  const enabled = !!pag.currentPageResults;
  return pag.nbPages > -1
    ? [
        searchButton,
        ...(ctrl.searching
          ? [searchInput]
          : [
              navButton(
                'First',
                licon.JumpFirst,
                () => ctrl.userSetPage(1),
                enabled && ctrl.page > 1,
                ctrl.redraw,
              ),
              navButton('Prev', licon.JumpPrev, ctrl.userPrevPage, enabled && ctrl.page > 1, ctrl.redraw),
              h('span.page', (pag.nbResults ? pag.from + 1 : 0) + '-' + pag.to + ' / ' + pag.nbResults),
              navButton(
                'Next',
                licon.JumpNext,
                ctrl.userNextPage,
                enabled && ctrl.page < pag.nbPages,
                ctrl.redraw,
              ),
              navButton(
                'Last',
                licon.JumpLast,
                ctrl.userLastPage,
                enabled && ctrl.page < pag.nbPages,
                ctrl.redraw,
              ),
              scrollToMeButton(ctrl),
            ]),
      ]
    : [];
}

export function pagerData<A>(ctrl: PaginatedCtrl<A>): PagerData<A> {
  const page = ctrl.page,
    nbResults = ctrl.data.nbPlayers,
    from = (page - 1) * maxPerPage,
    to = Math.min(nbResults, page * maxPerPage);
  return {
    from,
    to,
    currentPageResults: ctrl.pages[page],
    nbResults,
    nbPages: Math.ceil(nbResults / maxPerPage),
  };
}

export function myPage(ctrl: PaginatedCtrl<unknown>): number | undefined {
  return ctrl.data.me ? Math.floor((ctrl.data.me.rank - 1) / 10) + 1 : undefined;
}

export function searchButton(ctrl: PaginatedCtrl<unknown>): VNode {
  return h('button.fbt', {
    class: { active: ctrl.searching },
    attrs: { 'data-icon': ctrl.searching ? licon.X : licon.Search, title: 'Search tournament players' },
    hook: bind('click', ctrl.toggleSearch, ctrl.redraw),
  });
}

export function searchInput(ctrl: PaginatedCtrl<unknown>, completeOpts: Partial<UserCompleteOpts>): VNode {
  return h(
    'div.search',
    h('input', {
      attrs: { spellcheck: 'false' },
      hook: onInsert((el: HTMLInputElement) => {
        userComplete({
          input: el,
          tag: 'span',
          focus: true,
          onSelect(v) {
            ctrl.jumpToPageOf(v.id);
            ctrl.redraw();
          },
          ...completeOpts,
        });
        $(el).on('keydown', e => {
          if (e.code === 'Enter') {
            const rank = parseInt(e.target.value.replace('#', '').trim());
            if (rank > 0) ctrl.jumpToRank(rank);
          }
          if (e.code === 'Escape') {
            ctrl.toggleSearch();
            ctrl.redraw();
          }
        });
      }),
    }),
  );
}
