var m = require('mithril');
var throttle = require('common').throttle;

function editable(tag, submit) {
  return m('input', {
    spellcheck: false,
    value: tag[1],
    config: function(el, isUpdate, ctx) {
      if (isUpdate) return;
      el.onblur = function() {
        submit(el.value);
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

var selectedType;

function renderPgnTags(chapter, submit, node, types) {
  var rows = [
    ['Fen', m('pre#study_fen', node.fen)]
  ];
  if (chapter.setup.variant.key !== 'standard')
    rows.push(['Variant', fixed(chapter.setup.variant.name)]);
  rows = rows.concat(chapter.tags.map(function(tag) {
    return [
      tag[0],
      submit ? editable(tag, submit(tag[0])) : fixed(tag[1])
    ];
  }));
  if (submit) {
    var existingTypes = chapter.tags.map(function(t) {
      return t[0];
    });
    rows.push([
      m('select', {
        config: function(el) {
          selectedType = el.value;
        },
        onchange: function(e) {
          selectedType = e.target.value;
          $(e.target).parents('tr').find('input').focus();
        }
      }, [
        m('option', 'New tag'),
        types.map(function(t) {
          if (!lichess.fp.contains(existingTypes, t)) return m('option', {
            value: t
          }, t);
        })
      ]),
      editable(['', ''], function(value) {
        if (selectedType) submit(selectedType)(value);
      })
    ]);
  }

  return m('table.tags.slist', m('tbody', rows.map(function(r) {
    return m('tr', {
      key: r[0]
    }, [
      m('th', r[0]),
      m('td', r[1])
    ]);
  })));
}

module.exports = {
  ctrl: function(root, getChapter, members, types) {

    var submit = throttle(500, false, function(name, value) {
      root.study.makeChange('setTag', {
        chapterId: getChapter().id,
        name: name,
        value: value.substr(0, 140)
      });
    });

    return {
      submit: function(name) {
        return function(value) {
          submit(name, value);
        }
      },
      getChapter: getChapter,
      members: members,
      types: types,
      cacheKey: m.prop('')
    }
  },
  view: function(root) {
    var ctrl = root.tags,
      node = root.currentNode(),
      chapter = ctrl.getChapter(),
      canContribute = root.vm.mode.write;
    var key = [chapter.id, root.data.name, chapter.name, root.data.likes, chapter.tags, canContribute].join('|');
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
      canContribute && ctrl.submit,
      node,
      ctrl.types))
  }
};
