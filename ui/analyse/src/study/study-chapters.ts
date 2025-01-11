import { loadVendorScript } from 'common/assets';
import { type Prop, prop } from 'common/common';
import { hasTouchEvents } from 'common/mobile';
import { bind, dataIcon } from 'common/snabbdom';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import { iconTag, scrollTo } from '../util';
import { ctrl as chapterEditForm } from './chapter-edit-form';
import { type StudyChapterNewFormCtrl, ctrl as chapterNewForm } from './chapter-new-form';
import type {
  LocalPaths,
  StudyChapter,
  StudyChapterConfig,
  StudyChapterMeta,
  StudyCtrl,
  TagArray,
} from './interfaces';

export interface StudyChaptersCtrl {
  newForm: StudyChapterNewFormCtrl;
  editForm: any;
  list: Prop<StudyChapterMeta[]>;
  get(id: string): StudyChapterMeta | undefined;
  size(): number;
  sort(ids: string[]): void;
  firstChapterId(): string;
  toggleNewForm(): void;
  localPaths: LocalPaths;
}

export function ctrl(
  initChapters: StudyChapterMeta[],
  send: Socket.Send,
  setTab: () => void,
  chapterConfig: (id: string) => Promise<StudyChapterConfig>,
  root: AnalyseCtrl,
): StudyChaptersCtrl {
  const list: Prop<StudyChapterMeta[]> = prop(initChapters);

  const newForm = chapterNewForm(send, list, setTab, root);
  const editForm = chapterEditForm(send, chapterConfig, root.redraw);

  const localPaths: LocalPaths = {};

  return {
    newForm,
    editForm,
    list,
    get(id) {
      return list().find(c => c.id === id);
    },
    size() {
      return list().length;
    },
    sort(ids) {
      send('sortChapters', ids);
    },
    firstChapterId() {
      return list()[0].id;
    },
    toggleNewForm() {
      if (newForm.vm.open || list().length < 64) newForm.toggle();
      else alert('You have reached the limit of 64 chapters per study. Please create a new study.');
    },
    localPaths,
  };
}

export function isFinished(c: StudyChapter): boolean {
  const result = findTag(c.tags, 'result');
  return !!result && result !== '*';
}

export function findTag(tags: TagArray[], name: string): string | undefined {
  const t = tags.find(t => t[0].toLowerCase() === name);
  return t?.[1];
}

export function view(ctrl: StudyCtrl): VNode {
  const canContribute = ctrl.members.canContribute();
  const current = ctrl.currentChapter();

  function update(vnode: VNode) {
    const newCount = ctrl.chapters.list().length;
    const vData = vnode.data!.li!;
    const el = vnode.elm as HTMLElement;
    if (vData.count !== newCount) {
      if (current.id !== ctrl.chapters.firstChapterId()) {
        scrollTo(el, el.querySelector('.active'));
      }
    } else if (ctrl.vm.loading && vData.loadingId !== ctrl.vm.nextChapterId) {
      vData.loadingId = ctrl.vm.nextChapterId;
      scrollTo(el, el.querySelector('.loading'));
    }
    vData.count = newCount;
    if (canContribute && newCount > 1 && !vData.sortable) {
      const makeSortable = () => {
        vData.sortable = window.Sortable.create(el, {
          draggable: '.draggable',
          handle: hasTouchEvents ? 'span' : undefined,
          onSort() {
            ctrl.chapters.sort(vData.sortable.toArray());
          },
        });
      };
      if (window.Sortable) makeSortable();
      else loadVendorScript('sortablejs', 'Sortable.min.js').then(makeSortable);
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
              (target.parentNode as HTMLElement).getAttribute('data-id') ||
              target.getAttribute('data-id');
            if (!id) return;
            if (target.tagName === 'ACT') ctrl.chapters.editForm.toggle(ctrl.chapters.get(id));
            else ctrl.setChapter(id);
          });
          vnode.data!.li = {};
          update(vnode);
          window.lishogi.pubsub.emit('chat.resize');
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
        const editing = ctrl.chapters.editForm.isEditing(chapter.id);
        const loading = ctrl.vm.loading && chapter.id === ctrl.vm.nextChapterId;
        const active = !ctrl.vm.loading && current && current.id === chapter.id;
        return h(
          'div',
          {
            key: chapter.id,
            attrs: { 'data-id': chapter.id },
            class: { active, editing, loading, draggable: canContribute },
          },
          [
            h('span', loading ? h('span.ddloader') : [`${i + 1}`]),
            h('h3', chapter.name),
            canContribute ? h('act', { attrs: dataIcon('%') }) : null,
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
                [h('span', iconTag('O')), h('h3', i18n('study:addNewChapter'))],
              ),
            ]
          : [],
      ),
  );
}
