import { h } from 'snabbdom'
import { throttle } from 'common';
import AnalyseController from '../ctrl';

function editable(tag, submit) {
  return h('input', {
    attrs: {
      spellcheck: false,
      value: tag[1]
    },
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLInputElement;
        el.onblur = function() {
          submit(el.value);
        };
        el.onkeypress = function(e) {
          if ((e.keyCode || e.which) == 13) el.blur();
        }
      }
    }
  });
}

function fixed(text) {
  return h('span', text);
}

var selectedType;

function renderPgnTags(chapter, submit, node, types) {
  let rows = [
    ['Fen', h('pre#study_fen', node.fen)]
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
      h('select', {
        hook: {
          insert: vnode => {
            const el = vnode.elm as HTMLInputElement;
            selectedType = el.value;
            el.addEventListener('change', _ => {
              selectedType = el.value;
              $(el).parents('tr').find('input').focus();
            });
          },
          postpatch: (_, vnode) => {
            selectedType = (vnode.elm as HTMLInputElement).value;
          }
        }
      }, [
        h('option', 'New tag'),
        types.map(function(t) {
          if (!window.lichess.fp.contains(existingTypes, t)) return h('option', {
            attrs: { value: t }
          }, t);
        })
      ]),
      editable(['', ''], function(value) {
        if (selectedType) submit(selectedType)(value);
      })
    ]);
  }

  return h('table.tags.slist', h('tbody', rows.map(function(r) {
    return h('tr', {
      key: '' + r[0]
    }, [
      h('th', [r[0]]),
      h('td', [r[1]])
    ]);
  })));
}

export function ctrl(root: AnalyseController, getChapter, members, types) {

  const submit = throttle(500, false, function(name, value) {
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
    getChapter,
    members,
    types
  }
}

// TODO #cache
export function view(root) {
  const ctrl = root.tags,
  node = root.currentNode(),
  chapter = ctrl.getChapter(),
  canContribute = root.vm.mode.write;
  // const key = [chapter.id, root.data.name, chapter.name, root.data.likes, chapter.tags, canContribute].join('|');
  // if (key === ctrl.cacheKey() && m.redraw.strategy() === 'diff') {
  //   lichess.raf(function() {
  //     var el = document.getElementById('study_fen');
  //     if (el) el.textContent = node.fen;
  //   });
  //   return {
  //     subtree: 'retain'
  //   };
  // }
  return h('div.undertable_inner', renderPgnTags(
    chapter,
    canContribute && ctrl.submit,
    node,
    ctrl.types))
}
