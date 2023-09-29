import { prop, Prop, scrollToInnerSelector } from 'common';
import * as licon from 'common/licon';
import { bind, dataIcon, iconTag } from 'common/snabbdom';
import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { StudySocketSend } from '../socket';
import { ctrl as chapterEditForm, StudyChapterEditFormCtrl } from './chapterEditForm';
import { ctrl as chapterNewForm, StudyChapterNewFormCtrl } from './chapterNewForm';
import {
  LocalPaths,
  StudyChapter,
  StudyChapterConfig,
  StudyChapterMeta,
  StudyCtrl,
  TagArray,
} from './interfaces';

export default class StudyChaptersCtrl {
  newForm: StudyChapterNewFormCtrl;
  editForm: StudyChapterEditFormCtrl;
  list: Prop<StudyChapterMeta[]>;
  localPaths: LocalPaths = {};

  constructor(
    initChapters: StudyChapterMeta[],
    readonly send: StudySocketSend,
    setTab: () => void,
    chapterConfig: (id: string) => Promise<StudyChapterConfig>,
    root: AnalyseCtrl,
  ) {
    this.list = prop(initChapters);
    this.newForm = chapterNewForm(send, this.list, setTab, root);
    this.editForm = chapterEditForm(send, chapterConfig, root.trans, root.redraw);
  }

  get = (id: string) => this.list().find(c => c.id === id);
  sort = (ids: string[]) => this.send('sortChapters', ids);
  firstChapterId = () => this.list()[0].id;
  toggleNewForm = () => {
    if (this.newForm.vm.open || this.list().length < 64) this.newForm.toggle();
    else alert('You have reached the limit of 64 chapters per study. Please create a new study.');
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

export const looksLikeLichessGame = (tags: TagArray[]) =>
  !!findTag(tags, 'site')?.match(new RegExp(location.hostname + '/\\w{8}$'));

export function resultOf(tags: TagArray[], isWhite: boolean): string | undefined {
  switch (findTag(tags, 'result')) {
    case '1-0':
      return isWhite ? '1' : '0';
    case '0-1':
      return isWhite ? '0' : '1';
    case '1/2-1/2':
      return '1/2';
    default:
      return;
  }
}

export function view(ctrl: StudyCtrl): VNode {
  const canContribute = ctrl.members.canContribute(),
    current = ctrl.currentChapter();

  function update(vnode: VNode) {
    const newCount = ctrl.chapters.list().length,
      vData = vnode.data!.li!,
      el = vnode.elm as HTMLElement;
    if (vData.count !== newCount) {
      if (current.id !== ctrl.chapters.firstChapterId()) {
        scrollToInnerSelector(el, '.active');
      }
    } else if (ctrl.vm.loading && vData.loadingId !== ctrl.vm.nextChapterId) {
      vData.loadingId = ctrl.vm.nextChapterId;
      scrollToInnerSelector(el, '.loading');
    }
    vData.count = newCount;
    if (canContribute && newCount > 1 && !vData.sortable) {
      const makeSortable = function () {
        vData.sortable = window.Sortable.create(el, {
          draggable: '.draggable',
          handle: 'ontouchstart' in window ? 'span' : undefined,
          onSort() {
            ctrl.chapters.sort(vData.sortable.toArray());
          },
        });
      };
      if (window.Sortable) makeSortable();
      else lichess.loadIife('javascripts/vendor/Sortable.min.js').then(makeSortable);
    }
  }

  return h(
    'div.study__chapters',
    {
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLElement).addEventListener('click', e => {
            const target = e.target as HTMLElement;
            const id =
              (target.parentNode as HTMLElement).getAttribute('data-id') || target.getAttribute('data-id');
            if (!id) return;
            if (target.className === 'act') {
              const chapter = ctrl.chapters.get(id);
              if (chapter) ctrl.chapters.editForm.toggle(chapter);
            } else ctrl.setChapter(id);
          });
          vnode.data!.li = {};
          update(vnode);
          lichess.pubsub.emit('chat.resize');
        },
        postpatch(old, vnode) {
          vnode.data!.li = old.data!.li;
          update(vnode);
        },
        destroy: vnode => {
          const sortable = vnode.data!.li!.sortable;
          if (sortable) sortable.destroy();
        },
      },
    },
    ctrl.chapters
      .list()
      .map((chapter, i) => {
        const editing = ctrl.chapters.editForm.isEditing(chapter.id),
          loading = ctrl.vm.loading && chapter.id === ctrl.vm.nextChapterId,
          active = !ctrl.vm.loading && current && !ctrl.relay?.tourShow.active && current.id === chapter.id;
        return h(
          'div',
          {
            key: chapter.id,
            attrs: { 'data-id': chapter.id },
            class: { active, editing, loading, draggable: canContribute },
          },
          [
            h('span', loading ? h('span.ddloader') : ['' + (i + 1)]),
            h('h3', chapter.name),
            chapter.ongoing
              ? h('ongoing', { attrs: { ...dataIcon(licon.DiscBig), title: 'Ongoing' } })
              : null,
            !chapter.ongoing && chapter.res ? h('res', chapter.res) : null,
            canContribute ? h('i.act', { attrs: { ...dataIcon(licon.Gear), title: 'Edit chapter' } }) : null,
          ],
        );
      })
      .concat(
        ctrl.members.canContribute()
          ? [
              h(
                'div.add',
                {
                  hook: bind('click', ctrl.chapters.toggleNewForm, ctrl.redraw),
                },
                [h('span', iconTag(licon.PlusButton)), h('h3', ctrl.trans.noarg('addNewChapter'))],
              ),
            ]
          : [],
      ),
  );
}
