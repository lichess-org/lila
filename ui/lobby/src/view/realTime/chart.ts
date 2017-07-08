/// <reference types="types/lichess-jquery" />
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import LobbyController from '../../ctrl';
import { Hook } from '../../interfaces';
import { bind, perfIcons } from '../util';

function px(v) {
  return v + 'px';
}

function ratingLog(a) {
  return Math.log(a / 150 + 1);
}

function ratingY(e) {
  var rating = Math.max(1000, Math.min(2200, e || 1500));
  var ratio;
  var mid = 2/5;
  if (rating == 1500) {
    ratio = mid;
  } else if (rating > 1500) {
    ratio = mid + (ratingLog(rating - 1500) / ratingLog(1300)) * 2 * mid;
  } else {
    ratio = mid - (ratingLog(1500 - rating) / ratingLog(500)) * mid;
  }
  return Math.round(ratio * 489);
}

function clockX(dur) {
  function durLog(a) {
    return Math.log((a - 30) / 200 + 1);
  }
  var max = 2000;
  return Math.round(durLog(Math.min(max, dur || max)) / durLog(max) * 489);
}

function renderPlot(ctrl: LobbyController, hook: Hook) {
  const bottom = Math.max(0, ratingY(hook.rating) - 7),
  left = Math.max(0, clockX(hook.t) - 4),
  klass = [
    'plot.new',
    hook.ra ? 'rated' : 'casual',
    hook.action === 'cancel' ? 'cancel' : ''
  ].join('.');
  return h('span#' + hook.id + '.' + klass, {
    key: hook.id,
    attrs: {
      'data-icon': perfIcons[hook.perf],
      style: `bottom:${px(bottom)};left:${px(left)}`
    },
    hook: {
      insert(vnode) {
        $(vnode.elm as HTMLElement).powerTip({
          intentPollInterval: 100,
          placement: hook.rating > 1800 ? 'se' : 'ne',
          mouseOnToPopup: true,
          closeDelay: 200,
          popupId: 'hook'
        }).data('powertipjq', $(renderHook(ctrl, hook)));
        setTimeout(function() {
          (vnode.elm as HTMLElement).classList.remove('new');
        }, 20);
      },
      destroy(vnode) {
        $(vnode.elm as HTMLElement).data('powertipjq', null);
        $.powerTip.destroy(vnode.elm as HTMLElement);
      }
    }
  });
}

function renderHook(ctrl: LobbyController, hook: Hook) {
  let html = '';
  if (hook.rating) {
    html += '<a class="opponent ulpt" href="/@/' + hook.u + '">' + hook.u.substr(0, 14) + '</a>';
    html += '<span class="rating">' + hook.rating + (hook.prov ? '?' : '') + '</span>';
  } else {
    html += '<span class="opponent anon">Anonymous</span>';
  }
  html += '<span class="clock">' + hook.clock + '</span>';
  html += '<span class="mode">' +
  '<span class="varicon" data-icon="' + perfIcons[hook.perf] + '"></span>' + ctrl.trans(hook.ra ? 'rated' : 'casual') + '</span>';
  return html;
}

const xMarks = [1, 2, 3, 5, 7, 10, 15, 20, 30];

function renderXAxis() {
  const tags: VNode[] = [];
  xMarks.forEach(function(v) {
    const l = clockX(v * 60);
    tags.push(h('span.x.label', {
      attrs: { style: 'left:' + px(l) }
    }, '' + v));
    tags.push(h('div.grid.vert', {
      attrs: { style: 'width:' + px(l + 7) }
    }));
  });
  return tags;
}

const yMarks = [1000, 1200, 1400, 1500, 1600, 1800, 2000];

function renderYAxis() {
  const tags: VNode[] = [];
  yMarks.forEach(function(v) {
    const b = ratingY(v);
    tags.push(h('span.y.label', {
      attrs: { style: 'bottom:' + px(b + 5) }
    }, '' + v));
    tags.push(h('div.grid.horiz', {
      attrs: { style: 'height:' + px(b + 4) }
    }));
  });
  return tags;
}

export function toggle(ctrl: LobbyController) {
  return h('span.mode_toggle.hint--bottom', {
    key: 'set-mode-list',
    attrs: { 'data-hint': ctrl.trans('list') },
    hook: bind('mousedown', _ => ctrl.setMode('list'), ctrl.redraw)
  }, [
    h('span.chart', {
      attrs: { 'data-icon': '?' }
    })
  ]);
}

export function render(ctrl: LobbyController, hooks: Hook[]) {
  return h('div.hooks_chart', {
    key: 'chart'
  }, [
    h('div.canvas', {
      hook: bind('click', e => {
        if ((e.target as HTMLElement).classList.contains('plot')) ctrl.clickHook((e.target as HTMLElement).id);
      }, ctrl.redraw)
    }, hooks.map(hook => renderPlot(ctrl, hook))),
    ...renderYAxis(),
    ...renderXAxis()
  ]);
}
