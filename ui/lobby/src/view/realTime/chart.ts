import type LobbyController from '@/ctrl';
import * as licon from 'lib/licon';
import { bind } from 'lib/view';
import { h, type VNode } from 'snabbdom';
import type { Hook } from '@/interfaces';
import perfIcons from 'lib/game/perfIcons';

const percents = (v: number) => v + '%';

const ratingLog = (a: number) => Math.log(a / 150 + 1);

function ratingY(e?: number) {
  const rating = Math.max(1000, Math.min(2200, e || 1500));
  let ratio: number;
  const mid = 2 / 5;
  if (rating === 1500) {
    ratio = mid;
  } else if (rating > 1500) {
    ratio = mid + (ratingLog(rating - 1500) / ratingLog(1300)) * 2 * mid;
  } else {
    ratio = mid - (ratingLog(1500 - rating) / ratingLog(500)) * mid;
  }
  return Math.round(ratio * 92);
}

const clockMax = 2000;

const clockX = (dur: number) => {
  const durLog = (a: number) => Math.log((a - 30) / 200 + 1);
  return Math.round((durLog(Math.min(clockMax, dur || clockMax)) / durLog(clockMax)) * 100);
};

function renderPlot(ctrl: LobbyController, hook: Hook, translate: [number, number]) {
  const bottom = Math.max(0, ratingY(hook.rating) - translate[1]),
    left = Math.max(0, clockX(hook.t) - translate[0]),
    klass = [
      hook.id,
      'plot.new',
      hook.ra ? 'rated' : 'casual',
      hook.action === 'cancel' ? 'cancel' : '',
    ].join('.');
  return h('span#' + klass, {
    key: hook.id,
    attrs: { 'data-icon': perfIcons[hook.perf], style: `bottom:${percents(bottom)};left:${percents(left)}` },
    hook: {
      insert(vnode) {
        $(vnode.elm as HTMLElement).powerTip({
          placement: hook.rating && hook.rating > 1800 ? 'se' : 'ne',
          closeDelay: 200,
          popupId: 'hook',
          preRender() {
            $('#hook')
              .html(renderHook(ctrl, hook))
              .find('.inner-clickable')
              .on('click', () => ctrl.clickHook(hook.id));
          },
        });
        setTimeout(function () {
          (vnode.elm as HTMLElement).classList.remove('new');
        }, 20);
      },
      destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement),
    },
  });
}

function renderHook(ctrl: LobbyController, hook: Hook): string {
  let html = '<div class="inner">';
  if (hook.rating) {
    html += '<a class="opponent ulpt is color-icon" href="/@/' + hook.u + '">';
    html += ' ' + hook.u;
    if (ctrl.opts.showRatings) html += ' (' + hook.rating + (hook.prov ? '?' : '') + ')';
    html += '</a>';
  } else {
    html += '<span class="opponent anon">' + i18n.site.anonymous + '</span>';
  }
  html += '<div class="inner-clickable">';
  html += `<div>${hook.clock}</div>`;
  html += '<i data-icon="' + perfIcons[hook.perf] + '"> ' + i18n.site[hook.ra ? 'rated' : 'casual'] + '</i>';
  html += '</div></div>';
  return html;
}

const xMarks = [1, 2, 3, 5, 7, 10, 15, 20, 30];

function renderXAxis() {
  const tags: VNode[] = [];
  xMarks.forEach(v => {
    const l = clockX(v * 60);
    tags.push(h('span.x.label', { attrs: { style: 'left:' + percents(l - 1.5) } }, '' + v));
    tags.push(h('div.grid.vert', { attrs: { style: 'width:' + percents(l) } }));
  });
  return tags;
}

const yMarks = [1000, 1200, 1400, 1500, 1600, 1800, 2000];

function renderYAxis() {
  const tags: VNode[] = [];
  yMarks.forEach(function (v) {
    const b = ratingY(v);
    tags.push(h('span.y.label', { attrs: { style: 'bottom:' + percents(b + 1) } }, '' + v));
    tags.push(h('div.grid.horiz', { attrs: { style: 'height:' + percents(b + 0.8) } }));
  });
  return tags;
}

export function toggle(ctrl: LobbyController) {
  return h('i.toggle', {
    key: 'set-mode-list',
    attrs: { title: i18n.site.list, 'data-icon': licon.List },
    hook: bind('mousedown', _ => ctrl.setMode('list'), ctrl.redraw),
  });
}

export function render(ctrl: LobbyController, hooks: Hook[]) {
  let translate: [number, number] = [0, 0];
  const chart = document.querySelector('.hooks__chart') as HTMLElement;
  if (chart) {
    const fontSize = parseFloat(window.getComputedStyle(chart).fontSize);
    translate = [(fontSize / chart.clientWidth) * 95, (fontSize / chart.clientHeight) * 75];
  }

  return h('div.hooks__chart', [
    h(
      'div.canvas',
      {
        hook: bind(
          'click',
          e => {
            if ((e.target as HTMLElement).classList.contains('plot'))
              ctrl.clickHook((e.target as HTMLElement).id);
          },
          ctrl.redraw,
        ),
      },
      hooks.map(hook => renderPlot(ctrl, hook, translate)),
    ),
    ...renderYAxis(),
    ...renderXAxis(),
  ]);
}
