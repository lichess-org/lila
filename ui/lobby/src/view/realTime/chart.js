var m = require('mithril');
var util = require('chessground').util;

function px(v) {
  return v + 'px';
}

function ratingY(e) {
  function ratingLog(a) {
    return Math.log(a / 150 + 1);
  }
  var rating = Math.max(800, Math.min(2800, e || 1500));
  var ratio;
  if (rating == 1500) {
    ratio = 0.25;
  } else if (rating > 1500) {
    ratio = 0.25 + (ratingLog(rating - 1500) / ratingLog(1300)) * 3 / 4;
  } else {
    ratio = 0.25 - (ratingLog(1500 - rating) / ratingLog(500)) / 4;
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

function renderPlot(ctrl, hook) {
  var bottom = Math.max(0, ratingY(hook.rating) - 7);
  var left = Math.max(0, clockX(hook.time) - 4);
  var klass = [
    'plot',
    hook.mode == "Rated" ? 'rated' : 'casual',
    hook.action == 'cancel' ? 'cancel' : '',
    hook.variant != 'STD' ? 'variant' : ''
  ].join(' ');
  return m('span', {
    id: hook.id,
    key: hook.id,
    class: klass,
    style: {
      bottom: px(bottom),
      left: px(left)
    },
    config: function(el, isUpdate, ctx) {
      if (isUpdate) return;
      $(el).powerTip({
        fadeInTime: 0,
        fadeOutTime: 0,
        placement: hook.rating > 2200 ? 'se' : 'ne',
        mouseOnToPopup: true,
        closeDelay: 200,
        intentPollInterval: 50,
        popupId: 'hook'
      }).data('powertipjq', $(renderHook(ctrl, hook)));
      ctx.onunload = function() {
        $(el).data('powertipjq', null);
        $.powerTip.destroy(el);
      };
    }
  });
}

function renderHook(ctrl, hook) {
  var html = '';
  if (hook.rating) {
    html += '<a class="opponent" href="/@/' + hook.username + '">' + hook.username.substr(0, 14) + '</a>';
    html += '<span class="rating">' + hook.rating + '</span>';
  } else {
    html += '<span class="opponent anon">Anonymous</span>';
  }
  html += '<span class="clock">' + hook.clock + '</span>';
  html += '<span class="mode">' +
    '<span class="varicon" data-icon="' + hook.perf.icon + '"></span>' + ctrl.trans(hook.mode === 1 ? 'rated' : 'casual') + '</span>';
  html += '<span class="is is2 color-icon ' + (hook.color || "random") + '"></span>';
  return html;
}

function renderXAxis() {
  return [1, 2, 3, 5, 7, 10, 15, 20, 30].map(function(v) {
    var l = clockX(v * 60);
    return [
      m('span', {
        class: 'x label',
        style: {
          left: px(l)
        }
      }, v),
      m('div', {
        class: 'grid vert',
        style: {
          width: px(l + 7)
        }
      })
    ];
  });
}

function renderYAxis() {
  return [1000, 1200, 1400, 1500, 1600, 1800, 2000, 2200, 2400].map(function(v) {
    var b = ratingY(v);
    return [
      m('span', {
        class: 'y label',
        style: {
          bottom: px(b + 5)
        }
      }, v),
      m('div', {
        class: 'grid horiz',
        style: {
          height: px(b + 4)
        }
      })
    ];
  });
}

module.exports = {
  toggle: function(ctrl) {
    return m('span', {
      'data-hint': ctrl.trans('list'),
      class: 'mode_toggle hint--bottom',
      onclick: util.partial(ctrl.setMode, 'list')
    }, m('span.chart[data-icon=?]'));
  },
  render: function(ctrl, hooks) {
    return m('div.hooks_chart', [
      m('div.canvas', {
        onclick: function(e) {
          if (e.target.classList.contains('plot')) {
            ctrl.clickHook(e.target.id);
          }
        }
      }, hooks.map(util.partial(renderPlot, ctrl))),
      renderYAxis(),
      renderXAxis()
    ]);
  }
};
