var m = require('mithril');
var throttle = require('common').throttle;

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

function renderPgnTags(chapter, submit, node) {
  var rows = [
    ['Fen', m('pre#study_fen', node.fen)]
  ];
  if (chapter.setup.variant.key !== 'standard')
    rows.push(['Variant', fixed(chapter.setup.variant.name)]);
  rows = rows.concat(chapter.tags.map(function(tag) {
    return [
      tag[0],
      submit ? editable(tag, submit) : fixed(m.trust(urlToLink(tag[1])))
    ];
  }));

  return m('table.tags.slist', m('tbody', rows.map(function(r) {
    return m('tr', [
      m('th', r[0]),
      m('td', r[1])
    ]);
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
      members: members,
      cacheKey: m.prop('')
    }
  },
  view: function(root) {
    var ctrl = root.tags,
      node = root.currentNode(),
      chapter = ctrl.getChapter();
    var key = [chapter.id, root.data.name, chapter.name, root.data.likes, chapter.tags].join('|');
    if (key === ctrl.cacheKey() && m.redraw.strategy() === 'diff') {
      lichess.raf(function() {
        document.getElementById('study_fen').textContent = node.fen;
      });
      return {
        subtree: 'retain'
      };
    }
    ctrl.cacheKey(key);
    return m('div.undertable_inner', renderPgnTags(
      chapter,
      ctrl.members.canContribute() && ctrl.submit,
      node))
  }
};
