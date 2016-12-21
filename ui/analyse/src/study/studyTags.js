var m = require('mithril');
var throttle = require('common').throttle;

function renderTable(rows) {
  return m('table.tags.slist', m('tbody', rows.map(function(r) {
    if (r) return m('tr', [
      m('th', r[0]),
      m('td', r[1])
    ]);
  })));
}

function urlToLink(text) {
  var exp = /\bhttps?:\/\/(?:[a-z]{0,3}\.)?(lichess\.org[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
  return text.replace(exp, "<a href='//$1'>$1</a>");
}

function editable(tag, submit) {
  return m('input', {
    value: tag[1],
    config: function(el, isUpdate, ctx) {
      if (isUpdate) return;
      el.onblur = function() {
        submit(tag[0], el.value);
      };
      el.onkeypress = function(e) {
        if ((e.keyCode || e.which) == '13') el.blur();
      }
    }
  });
}

function fixed(text) {
  return m('span', text);
}

function renderPgnTags(chapter, submit) {
  return renderTable([
    ['Fen', m('pre#study_fen', '[current]')],
    ['Variant', fixed(chapter.setup.variant.name)]
  ].concat(chapter.tags.map(function(tag) {
    if (tag[0].toLowerCase() !== 'fen') return [
      tag[0],
      submit ? editable(tag, submit) : fixed(m.trust(urlToLink(tag[1])))
    ];
  })));
}

var lastCacheKey;

module.exports = {
  ctrl: function(root, getChapter, members) {

    var submit = throttle(500, false, function(name, value) {
      root.study.contribute('setTag', {
        chapterId: getChapter().id,
        name: name,
        value: value
      });
    });

    return {
      submit: submit,
      getChapter: getChapter,
      members: members
    }
  },
  view: function(ctrl) {
    // lichess.raf(function() {
    //   var n = document.getElementById('study_fen');
    //   if (n) n.textContent = ctrl.currentNode().fen;
    // });
    // var cacheKey = [chapter.id, d.name, chapter.name, d.likes].join('|');
    // if (cacheKey === lastCacheKey && m.redraw.strategy() === 'diff') {
    //   return {
    //     subtree: 'retain'
    //   };
    // }
    // lastCacheKey = cacheKey;
    return renderPgnTags(
      ctrl.getChapter(),
      ctrl.members.canContribute() && ctrl.submit)
  }
};
