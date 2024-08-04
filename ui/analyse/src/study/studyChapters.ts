import { defined, prop, Prop, scrollToInnerSelector } from 'common';
import * as licon from 'common/licon';
import { bind, dataIcon, iconTag, looseH as h } from 'common/snabbdom';
import { VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { StudySocketSend } from '../socket';
import { StudyChapterEditForm } from './chapterEditForm';
import { StudyChapterNewForm } from './chapterNewForm';
import {
  LocalPaths,
  StudyChapter,
  StudyChapterConfig,
  ChapterPreview,
  TagArray,
  ServerNodeMsg,
  ChapterPreviewFromServer,
  ChapterId,
  Federations,
  ChapterPreviewPlayerFromServer,
  ChapterPreviewPlayer,
  ChapterSelect,
} from './interfaces';
import StudyCtrl from './studyCtrl';
import { opposite } from 'chessops/util';
import { fenColor } from 'common/miniBoard';
import { initialFen } from 'chess';
import type Sortable from 'sortablejs';

/* read-only interface for external use */
export class StudyChapters {
  constructor(private readonly list: Prop<ChapterPreview[]>) {}
  all = () => this.list();
  get = (id: ChapterId | number) => {
    const str = id.toString();
    const number = str.length < 4 && parseInt(str);
    return number ? this.list()[number - 1] : this.list().find(c => c.id === id);
  };
  size = () => this.list().length;
  first = () => this.list()[0];
  looksNew = () => {
    const cs = this.all();
    return cs.length === 1 && cs[0].name == 'Chapter 1';
  };
}

export default class StudyChaptersCtrl {
  store: Prop<ChapterPreview[]> = prop([]);
  list: StudyChapters;
  newForm: StudyChapterNewForm;
  editForm: StudyChapterEditForm;
  localPaths: LocalPaths = {};

  constructor(
    initChapters: ChapterPreviewFromServer[],
    readonly send: StudySocketSend,
    setTab: () => void,
    chapterConfig: (id: string) => Promise<StudyChapterConfig>,
    private readonly federations: () => Federations | undefined,
    root: AnalyseCtrl,
  ) {
    this.list = new StudyChapters(this.store);
    this.loadFromServer(initChapters);
    this.newForm = new StudyChapterNewForm(send, this.list, setTab, root);
    this.editForm = new StudyChapterEditForm(send, chapterConfig, root.trans, root.redraw);
  }

  sort = (ids: string[]) => this.send('sortChapters', ids);
  toggleNewForm = () => {
    if (this.newForm.isOpen() || this.list.size() < 64) this.newForm.toggle();
    else alert('You have reached the limit of 64 chapters per study. Please create a new study.');
  };
  loadFromServer = (chapters: ChapterPreviewFromServer[]) =>
    this.store(
      chapters.map(c => ({
        ...c,
        fen: c.fen || initialFen,
        players: c.players ? this.convertPlayersFromServer(c.players) : undefined,
        orientation: c.orientation || 'white',
        variant: c.variant || 'standard',
        playing: defined(c.lastMove) && c.status === '*',
        lastMoveAt: defined(c.thinkTime) ? Date.now() - 1000 * c.thinkTime : undefined,
      })),
    );
  private convertPlayersFromServer = (players: PairOf<ChapterPreviewPlayerFromServer>) => {
    const feds = this.federations(),
      conv: ChapterPreviewPlayer[] = players.map(p => ({
        ...p,
        fed: p.fed ? { id: p.fed, name: feds?.[p.fed] || p.fed } : undefined,
      }));
    return { white: conv[0], black: conv[1] };
  };

  addNode = (d: ServerNodeMsg) => {
    const pos = d.p,
      node = d.n;
    const cp = this.list.get(pos.chapterId);
    if (cp) {
      const onRelayPath = d.relayPath == d.p.path + d.n.id;
      if (onRelayPath || !d.relayPath) {
        cp.fen = node.fen;
        cp.lastMove = node.uci;
        cp.check = node.san?.includes('#') ? '#' : node.san?.includes('+') ? '+' : undefined;
      }
      if (onRelayPath) {
        cp.lastMoveAt = Date.now();
        const playerWhoMoved = cp.players?.[opposite(fenColor(cp.fen))];
        if (playerWhoMoved) playerWhoMoved.clock = node.clock;
      }
    }
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

export const gameLinkAttrs = (roundPath: string, game: { id: ChapterId }) => ({
  href: `${roundPath}/${game.id}`,
});
export const gameLinksListener = (select: ChapterSelect) => (vnode: VNode) =>
  (vnode.elm as HTMLElement).addEventListener(
    'click',
    async e => {
      let target = e.target as HTMLLinkElement;
      while (target && target.tagName !== 'A') target = target.parentNode as HTMLLinkElement;
      const href = target?.href;
      const id = target?.dataset['board'] || href?.match(/^[^?#]*/)?.[0].slice(-8);
      if (id && select.is(id)) {
        if (!href?.match(/[?&]embed=/)) e.preventDefault();
        await select.set(id);
      }
    },
    { passive: false },
  );

export function view(ctrl: StudyCtrl): VNode {
  const canContribute = ctrl.members.canContribute(),
    current = ctrl.currentChapter();
  function update(vnode: VNode) {
    const newCount = ctrl.chapters.list.size(),
      vData = vnode.data!.li!,
      el = vnode.elm as HTMLElement;
    if (vData.count !== newCount) {
      if (current.id !== ctrl.chapters.list.first().id) {
        scrollToInnerSelector(el, '.active');
      }
    } else if (vData.currentId !== ctrl.data.chapter.id) {
      vData.currentId = ctrl.data.chapter.id;
      scrollToInnerSelector(el, '.active');
    }
    vData.count = newCount;
    if (canContribute && newCount > 1 && !vData.sortable) {
      site.asset.loadEsm<typeof Sortable>('sortable.esm', { npm: true }).then(s => {
        vData.sortable = s.create(el, {
          draggable: '.draggable',
          handle: 'ontouchstart' in window ? 'span' : undefined,
          onSort: () => ctrl.chapters.sort(vData.sortable.toArray()),
        });
      });
    }
  }

  return h(
    'div.study__chapters',
    {
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLElement).addEventListener('click', e => {
            const target = e.target as HTMLElement;
            const id = (target.parentNode as HTMLElement).dataset['id'] || target.dataset['id'];
            if (!id) return;
            if (target.className === 'act') {
              const chapter = ctrl.chapters.list.get(id);
              if (chapter) ctrl.chapters.editForm.toggle(chapter);
            } else ctrl.setChapter(id);
          });
          vnode.data!.li = {};
          update(vnode);
          site.pubsub.emit('chat.resize');
        },
        postpatch(old, vnode) {
          vnode.data!.li = old.data!.li;
          update(vnode);
        },
        destroy: vnode => {
          const sortable: Sortable = vnode.data!.li!.sortable;
          if (sortable) sortable.destroy();
        },
      },
    },
    ctrl.chapters.list
      .all()
      .map((chapter, i) => {
        const editing = ctrl.chapters.editForm.isEditing(chapter.id),
          active = !ctrl.vm.loading && current?.id === chapter.id;
        return h(
          'div',
          {
            key: chapter.id,
            attrs: { 'data-id': chapter.id },
            class: { active, editing, draggable: canContribute },
          },
          [
            h('span', (i + 1).toString()),
            h('h3', chapter.name),
            chapter.status && h('res', chapter.status),
            canContribute && h('i.act', { attrs: { ...dataIcon(licon.Gear), title: 'Edit chapter' } }),
          ],
        );
      })
      .concat(
        ctrl.members.canContribute()
          ? [
              h('div.add', { hook: bind('click', ctrl.chapters.toggleNewForm, ctrl.redraw) }, [
                h('span', iconTag(licon.PlusButton)),
                h('h3', ctrl.trans.noarg('addNewChapter')),
              ]),
            ]
          : [],
      ),
  );
}
