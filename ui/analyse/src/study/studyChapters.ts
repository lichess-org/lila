import { defined, prop, type Prop, scrollToInnerSelector } from 'lib';
import * as licon from 'lib/licon';
import { type VNode, bind, dataIcon, iconTag, hl, alert } from 'lib/view';
import type AnalyseCtrl from '../ctrl';
import type { StudySocketSend } from '../socket';
import { StudyChapterEditForm } from './chapterEditForm';
import { StudyChapterNewForm } from './chapterNewForm';
import type {
  LocalPaths,
  StudyChapter,
  StudyChapterConfig,
  ChapterPreview,
  TagArray,
  ServerNodeMsg,
  ChapterPreviewFromServer,
  ChapterId,
  Federations,
  StudyPlayerFromServer,
  StudyPlayer,
  ChapterSelect,
  StatusStr,
} from './interfaces';
import type StudyCtrl from './studyCtrl';
import { opposite } from 'chessops/util';
import { fenColor } from 'lib/game/chess';
import type Sortable from 'sortablejs';
import { INITIAL_FEN } from 'chessops/fen';

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
    return cs.length === 1 && cs[0].name === 'Chapter 1';
  };
}

export default class StudyChaptersCtrl {
  store: Prop<ChapterPreview[]> = prop([]);
  list: StudyChapters;
  newForm: StudyChapterNewForm;
  editForm: StudyChapterEditForm;
  localPaths: LocalPaths = {};
  scroller = new StudyChapterScroller();

  constructor(
    initChapters: ChapterPreviewFromServer[],
    readonly send: StudySocketSend,
    readonly isBroadcast: boolean,
    setTab: () => void,
    chapterConfig: (id: string) => Promise<StudyChapterConfig>,
    private readonly federations: () => Federations | undefined,
    root: AnalyseCtrl,
  ) {
    this.list = new StudyChapters(this.store);
    this.loadFromServer(initChapters);
    this.newForm = new StudyChapterNewForm(send, this.list, isBroadcast, setTab, root);
    this.editForm = new StudyChapterEditForm(send, chapterConfig, isBroadcast, root.redraw);
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
        fen: c.fen || INITIAL_FEN,
        players: c.players ? this.convertPlayersFromServer(c.players) : undefined,
        orientation: c.orientation || 'white',
        variant: c.variant || 'standard',
        playing: defined(c.lastMove) && c.status === '*',
        lastMoveAt: defined(c.thinkTime) ? Date.now() - 1000 * c.thinkTime : undefined,
      })),
    );
  private convertPlayersFromServer = (players: PairOf<StudyPlayerFromServer>) => {
    const feds = this.federations(),
      conv: StudyPlayer[] = players.map(p => convertPlayerFromServer(p, feds));
    return { white: conv[0], black: conv[1] };
  };

  addNode = (d: ServerNodeMsg) => {
    const pos = d.p,
      node = d.n;
    const cp = this.list.get(pos.chapterId);
    if (cp) {
      const onRelayPath = d.relayPath === d.p.path + d.n.id;
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

  setTags = (id: ChapterId, tags: TagArray[]) => {
    const chap = this.list.get(id),
      result = findTag(tags, 'result');
    if (chap && result) chap.status = result.replace(/1\/2/g, '½') as StatusStr;
  };

  hasPlayingChapter = () => this.list.all().some(c => c.playing);
}

export const convertPlayerFromServer = <A extends StudyPlayerFromServer>(
  player: A,
  federations?: Federations,
) => ({
  ...player,
  fed: player.fed ? { id: player.fed, name: federations?.[player.fed] || player.fed } : undefined,
});

export function isFinished(c: StudyChapter) {
  const result = findTag(c.tags, 'result');
  return !!result && result !== '*';
}

export const findTag = (tags: TagArray[], name: string) => tags.find(t => t[0].toLowerCase() === name)?.[1];

export const looksLikeLichessGame = (tags: TagArray[]) =>
  !!findTag(tags, 'site')?.match(new RegExp(location.hostname + '/\\w{8}$'));

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

function onListUpdate(ctrl: StudyCtrl, vnode: VNode) {
  const vData = vnode.data!.li!,
    el = vnode.elm as HTMLElement;
  ctrl.chapters.scroller.scrollIfNeeded(el);
  if (ctrl.members.canContribute() && ctrl.chapters.list.size() > 1 && !vData.sortable) {
    site.asset.loadEsm<typeof Sortable>('sortable.esm', { npm: true }).then(s => {
      vData.sortable = s.create(el, {
        draggable: '.draggable',
        handle: 'ontouchstart' in window ? 'span' : undefined,
        onSort: () => ctrl.chapters.sort(vData.sortable.toArray()),
      });
    });
  }
}

export function view(ctrl: StudyCtrl): VNode {
  const canContribute = ctrl.members.canContribute(),
    current = ctrl.currentChapter();

  return hl('div.study__chapters', [
    hl(
      'div.study-list',
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
            onListUpdate(ctrl, vnode);
          },
          postpatch(old, vnode) {
            vnode.data!.li = old.data!.li;
            onListUpdate(ctrl, vnode);
          },
          destroy: vnode => {
            const sortable: Sortable = vnode.data!.li!.sortable;
            if (sortable) sortable.destroy();
          },
        },
      },
      ctrl.chapters.list.all().map((chapter, i) => {
        const editing = ctrl.chapters.editForm.isEditing(chapter.id),
          active = !ctrl.vm.loading && current?.id === chapter.id;
        return hl(
          'button',
          {
            key: chapter.id,
            attrs: { 'data-id': chapter.id },
            class: { active, editing, draggable: canContribute },
          },
          [
            hl('span', (i + 1).toString()),
            hl('h3', chapter.name),
            chapter.status && hl('res', chapter.status),
            canContribute &&
              hl('i.act', { attrs: { ...dataIcon(licon.Gear), title: i18n.study.editChapter } }),
          ],
        );
      }),
    ),
    ctrl.members.canContribute() &&
      hl('button.add', { hook: bind('click', ctrl.chapters.toggleNewForm, ctrl.redraw) }, [
        hl('span', iconTag(licon.PlusButton)),
        hl('h3', i18n.study.addNewChapter),
      ]),
  ]);
}

export class StudyChapterScroller {
  request: Prop<ScrollBehavior | null> = prop('instant');
  private rafId?: number;

  scrollIfNeeded(list: HTMLElement) {
    const request = this.request();
    if (!request) return;
    const active = list.querySelector('.active');
    if (!active) return;
    this.request(null);
    const [c, l] = [list.getBoundingClientRect(), active.getBoundingClientRect()];
    if (c.top < l.top || c.bottom > l.bottom) {
      cancelAnimationFrame(this.rafId ?? 0);
      this.rafId = requestAnimationFrame(() => {
        scrollToInnerSelector(list, '.active', false, request);
        this.rafId = undefined;
      });
    }
  }
}
