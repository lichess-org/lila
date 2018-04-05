import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { prop, Prop } from 'common';
import { bind, dataIcon, iconTag } from '../util';
import { ctrl as chapterNewForm } from './chapterNewForm';
import { ctrl as chapterEditForm } from './chapterEditForm';
import AnalyseCtrl from '../ctrl';
import { StudyCtrl, StudyChapterMeta, LocalPaths, StudyChapter, TagArray } from './interfaces';

export function ctrl(initChapters: StudyChapterMeta[], send: SocketSend, setTab: () => void, chapterConfig, root: AnalyseCtrl) {

  const list: Prop<StudyChapterMeta[]> = prop(initChapters);

  const newForm = chapterNewForm(send, list, setTab, root);
  const editForm = chapterEditForm(send, chapterConfig, root.redraw);

  const localPaths: LocalPaths = {};

  return {
    newForm,
    editForm,
    list,
    get(id) {
      return list().find(function(c) {
        return c.id === id;
      });
    },
    size() {
      return list().length;
    },
    sort(ids) {
      send("sortChapters", ids);
    },
    firstChapterId() {
      return list()[0].id;
    },
    toggleNewForm() {
      if (newForm.vm.open || list().length < 64) newForm.toggle();
      else alert("You have reached the limit of 64 chapters per study. Please create a new study.");
    },
    localPaths
  };
}

export function isFinished(c: StudyChapter) {
  const result = findTag(c.tags, 'result');
  return result && result !== '*';
}

export function findTag(tags: TagArray[], name: string): string | undefined {
  const t = tags.find(t => t[0].toLowerCase() === name);
  return t && t[1];
}

export function resultOf(tags: TagArray[], isWhite: boolean): string | undefined {
  switch(findTag(tags, 'result')) {
    case '1-0': return isWhite ? '1' : '0';
    case '0-1': return isWhite ? '0' : '1';
    case '1/2-1/2': return '1/2';
    default: return;
  }
}

export function view(ctrl: StudyCtrl): VNode {

  const configButton = ctrl.members.canContribute() ? h('i.action.config', { attrs: dataIcon('%') }) : null;
  const current = ctrl.currentChapter();

  function update(vnode: VNode) {
    const newCount = ctrl.chapters.list().length;
    const vData = vnode.data!.li!;
    const el = vnode.elm as HTMLElement;
    if (vData.count !== newCount) {
      if (current.id !== ctrl.chapters.firstChapterId()) {
        $(el).scrollTo($(el).find('.active'), 200);
      }
    } else if (ctrl.vm.loading && vData.loadingId !== ctrl.vm.nextChapterId) {
      vData.loadingId = ctrl.vm.nextChapterId;
      const ch = $(el).find('.loading');
      if (ch.length) $(el).scrollTo(ch, 200);
    }
    vData.count = newCount;
    if (ctrl.members.canContribute() && newCount > 1 && !vData.sortable) {
      const makeSortable = function() {
        vData.sortable = window['Sortable'].create(el, {
          draggable: '.draggable',
          onSort: function() {
            ctrl.chapters.sort(vData.sortable.toArray());
          }
        });
      }
      if (window['Sortable']) makeSortable();
      else window.lichess.loadScript('/assets/javascripts/vendor/Sortable.min.js').done(makeSortable);
    }
  }

  return h('div.list.chapters', {
    hook: {
      insert(vnode) {
        (vnode.elm as HTMLElement).addEventListener('click', e => {
          const target = e.target as HTMLElement;
          const id = (target.parentNode as HTMLElement).getAttribute('data-id') || target.getAttribute('data-id');
          if (!id) return;
          if (target.classList.contains('config')) ctrl.chapters.editForm.toggle(ctrl.chapters.get(id));
          else ctrl.setChapter(id);
        });
        vnode.data!.li = {};
        update(vnode);
      },
      postpatch(old, vnode) {
        vnode.data!.li = old.data!.li;
        update(vnode);
      },
      destroy: vnode => {
        const sortable = vnode.data!.li!.sortable;
        if (sortable) sortable.destroy()
      }
    }
  },
  ctrl.chapters.list().map(function(chapter, i) {
    const editing = ctrl.chapters.editForm.isEditing(chapter.id),
    loading = ctrl.vm.loading && chapter.id === ctrl.vm.nextChapterId,
    active = !ctrl.vm.loading && current && current.id === chapter.id;
    return h('div.elem.chapter.draggable', {
      key: chapter.id,
      attrs: { 'data-id': chapter.id },
      class: { active, editing, loading }
    }, [
      h('span.status', loading ? h('span.ddloader', i + 1) : i + 1),
      h('h3', chapter.name),
      configButton
    ]);
  }).concat([
    ctrl.members.canContribute() ? h('div.elem.chapter.add', {
      hook: bind('click', ctrl.chapters.toggleNewForm, ctrl.redraw)
    }, [
      h('span.status', iconTag('O')),
      h('h3.add_text', 'Add a new chapter')
    ]) : null
  ]));
}
