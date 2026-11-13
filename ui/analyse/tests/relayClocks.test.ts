import assert from 'node:assert/strict';
import { registerHooks } from 'node:module';
import { mock, test } from 'node:test';
import type { VNode } from 'snabbdom';

import { completeNode } from 'lib/tree/node';
import { makeTree } from 'lib/tree/tree';

import type AnalyseCtrl from '../src/ctrl';
import type { AnalyseData } from '../src/interfaces';
import type { ChapterPreviewFromServer, StudyDataFromServer, TagArray } from '../src/study/interfaces';
import type { RelayData } from '../src/study/relay/interfaces';

// The test runner's @/ resolver requires package exports, which analyse does not have.
const analyseSrc = new URL('../src/', import.meta.url).href;
registerHooks({
  resolve(specifier, context, nextResolve) {
    if (specifier.startsWith('@/') && context.parentURL?.startsWith(analyseSrc))
      return nextResolve(new URL(`${specifier.slice(2)}.ts`, analyseSrc).href, context);
    return nextResolve(specifier, context);
  },
});
mock.module(new URL('../../lib/src/permalog.ts', import.meta.url).href, {
  namedExports: { log: async () => {} },
});
Object.assign(site, { asset: { flairSrc: () => 'pin.svg' }, mousetrap: { bind: () => {} } });
window.site = site;
document.documentElement.lang = 'en';
window.history.replaceState({}, '', '/broadcast/test/tour0001/round001/chapterA');
Object.defineProperty(globalThis, 'location', { value: window.location, configurable: true });

const { default: StudyCtrl } = await import('../src/study/studyCtrl');
const { renderClock, previewContent } = await import('../src/study/multiBoard');
const { gamesLists } = await import('../src/study/relay/relayGames');
const { default: renderClocks } = await import('../src/view/clocks');

const now = 1_000_000;
const initialFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
const blackFen = 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1';
const whiteFen = 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2';
const nextBlackFen = 'rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKBNR b KQkq - 1 2';

function fixture(
  opts: { age?: number | null; chapterAge?: number | null; thinkTime?: number; relay?: boolean } = {},
) {
  const age = opts.age === null ? undefined : (opts.age ?? 20_000);
  const chapterAge = opts.chapterAge === null ? undefined : (opts.chapterAge ?? age);
  const ageField = age === undefined ? {} : { clockAge: age };
  const chapterAgeField = chapterAge === undefined ? {} : { clockAge: chapterAge };
  const previews: ChapterPreviewFromServer[] = ['chapterA', 'chapterB'].map(id => ({
    id,
    name: id,
    fen: whiteFen,
    lastMove: 'e7e5',
    status: '*',
    thinkTime: opts.thinkTime ?? 120,
    players: [
      { name: 'White', clock: 6_000 },
      { name: 'Black', clock: 8_000 },
    ],
    ...ageField,
  }));
  const data: StudyDataFromServer = {
    id: 'round001',
    name: 'Round',
    members: {},
    position: { chapterId: 'chapterA', path: 'aabb' },
    ownerId: 'owner',
    settings: {
      computer: 'nobody',
      explorer: 'nobody',
      cloneable: 'everyone',
      shareable: 'everyone',
      chat: 'nobody',
    },
    visibility: 'public',
    createdAt: 0,
    from: 'broadcast',
    likes: 0,
    liked: false,
    features: { cloneable: true, shareable: true, chat: false, sticky: false },
    chapter: {
      id: 'chapterA',
      name: 'chapterA',
      ownerId: 'owner',
      setup: { variant: { key: 'standard', name: 'Standard' }, orientation: 'white' },
      tags: [['Result', '*']],
      practice: false,
      gamebook: false,
      features: { computer: false, explorer: false },
      relayPath: 'aabb',
      ...chapterAgeField,
    },
    secondsSinceUpdate: 0,
    admin: false,
    showRatings: true,
    chapters: previews,
  };
  const relay: RelayData | undefined =
    opts.relay === false
      ? undefined
      : {
          tour: { id: 'tour0001', name: 'Tour', slug: 'test', info: {} },
          rounds: [{ id: 'round001', name: 'Round', slug: 'round', url: '', ongoing: true }],
          photos: {},
        };
  const root = completeNode('standard')({
    ply: 0,
    fen: initialFen,
    clock: 9_000,
    children: [
      {
        id: 'aa',
        ply: 1,
        fen: blackFen,
        uci: 'e2e4',
        clock: 6_000,
        children: [{ id: 'bb', ply: 2, fen: whiteFen, uci: 'e7e5', clock: 8_000 }],
      },
    ],
  });
  const tree = makeTree(root);
  const redraw = mock.fn();
  const ctrl = {
    opts: {},
    socket: { send: () => {} },
    redraw,
    requestInitialPly: 0,
    path: 'aabb',
    node: tree.nodeAtPath('aabb'),
    mainline: tree.getNodeList('aabb'),
    tree,
    variantKey: 'standard',
    isEmbed: true,
    bottomIsWhite: () => true,
    autoplay: {},
    explorer: { disable: () => {}, allowed: () => {} },
    reloadData: () => {},
    startCevalIfEnabled: () => {},
  } as unknown as AnalyseCtrl;
  ctrl.userJump = path => {
    ctrl.path = path;
    ctrl.node = tree.nodeAtPath(path);
  };
  ctrl.jump = ctrl.userJump;
  const study = new StudyCtrl(data, ctrl, [], undefined, relay);
  ctrl.study = study;
  return { study, ctrl, tree, data, redraw };
}

function text(node: VNode | string | number | undefined): string {
  if (node === undefined) return '';
  if (typeof node !== 'object') return String(node);
  return node.text ?? node.children?.map(text).join('') ?? '';
}

function clocksIn(node: VNode): VNode[] {
  const found = node.sel?.startsWith('span.mini-game__clock') ? [node] : [];
  for (const child of node.children ?? [])
    if (typeof child === 'object' && child) found.push(...clocksIn(child));
  return found;
}

function mainClocks(ctrl: AnalyseCtrl, path = 'aabb'): string[] {
  return renderClocks(ctrl, path)!.map(text);
}

function reload(
  study: InstanceType<typeof StudyCtrl>,
  data: StudyDataFromServer,
  tags: TagArray[],
  age?: number,
) {
  study.onReload({
    analysis: {} as AnalyseData,
    study: { ...data, chapters: undefined, chapter: { ...data.chapter, tags, clockAge: age } },
  });
}

test('snapshots use one reference across the tree, multiboard and sidebar without double deduction', t => {
  t.mock.timers.enable({ apis: ['Date', 'setInterval'], now });
  const { study, ctrl, data } = fixture();
  const chapter = study.currentChapter();
  assert.equal(chapter.lastMoveAt, now - 120_000);
  assert.equal(chapter.clockAt, now - 20_000);
  assert.deepEqual(mainClocks(ctrl), ['0:00:40', '0:01:20']);
  assert.deepEqual(
    previewContent(chapter, 'white', undefined, true, study.relay!.round).flatMap(clocksIn).map(text),
    ['1:20', '0:40'],
  );
  assert.deepEqual(clocksIn(gamesLists(study, study.relay!)).slice(0, 2).map(text), ['1:20', '0:40']);

  t.mock.timers.tick(5_000);
  assert.deepEqual(mainClocks(ctrl), ['0:00:35', '0:01:20']);
  study.socketHandler('chapters', [
    { ...data.chapters![0], thinkTime: 125, clockAge: 25_000 },
    data.chapters![1],
  ]);
  assert.equal(study.currentChapter().clockAt, now - 20_000);
  assert.equal(text(renderClock(study.currentChapter(), 'white', true)), '0:35');
  assert.deepEqual(mainClocks(ctrl), ['0:00:35', '0:01:20']);

  const preview = study.currentChapter();
  reload(study, data, [['Result', '*']], 25_000);
  assert.equal(study.currentChapter(), preview, 'reload without chapters retains the preview');
  assert.equal(preview.clockAt, now - 20_000);
  assert.deepEqual(mainClocks(ctrl), ['0:00:35', '0:01:20']);
  assert.equal(text(renderClock(preview, 'white', true)), '0:35');

  study.socketHandler('chapters', [
    { ...data.chapters![0], thinkTime: 125, clockAge: undefined },
    data.chapters![1],
  ]);
  assert.equal(study.currentChapter().clockAt, undefined);
  assert.equal(mainClocks(ctrl)[0], '0:00:35');
  assert.equal(text(renderClock(study.currentChapter(), 'white', true)), '0:00');
});

test('the loaded chapter and preview keep their own clock origins', t => {
  t.mock.timers.enable({ apis: ['Date', 'setInterval'], now });
  const { study, ctrl, data } = fixture({ age: 20_000, chapterAge: 0, thinkTime: 15 });
  const chapter = study.currentChapter();
  assert.equal(chapter.clockAt, now - 20_000);
  assert.deepEqual(mainClocks(ctrl), ['0:01:00', '0:01:20']);
  assert.equal(text(renderClock(chapter, 'white', true)), '0:40');

  t.mock.timers.tick(5_000);
  reload(study, data, [['Result', '*']], 30_000);
  assert.equal(study.currentChapter(), chapter);
  assert.equal(chapter.clockAt, now - 20_000);
  assert.equal(mainClocks(ctrl)[0], '0:00:30');
  assert.equal(text(renderClock(chapter, 'white', true)), '0:35');

  reload(study, data, [['Result', '*']]);
  assert.equal(chapter.clockAt, now - 20_000);
  assert.equal(mainClocks(ctrl)[0], '0:00:40');
  assert.equal(text(renderClock(chapter, 'white', true)), '0:35');

  const legacy = fixture({ age: 20_000, chapterAge: null, thinkTime: 15 });
  assert.equal(legacy.study.currentChapter().clockAt, now - 15_000);
  assert.equal(mainClocks(legacy.ctrl)[0], '0:00:45');
});

test('tree and preview retain matching values and origins when their snapshots differ', t => {
  t.mock.timers.enable({ apis: ['Date', 'setInterval'], now });
  const { study, ctrl } = fixture({ age: 0, chapterAge: 20_000 });
  const chapter = study.currentChapter();
  chapter.players!.white.clock = 5_000;
  assert.equal(mainClocks(ctrl)[0], '0:00:40');
  assert.equal(text(renderClock(chapter, 'white', true)), '0:50');
});

test('clock events distinguish absent, zero, future and cleared origins and clocks', t => {
  t.mock.timers.enable({ apis: ['Date', 'setInterval'], now });
  const { study, ctrl, tree } = fixture({ age: null, thinkTime: 15 });
  const chapter = study.currentChapter();
  const p = { chapterId: chapter.id, path: 'aa' };
  assert.equal(chapter.clockAt, undefined);
  assert.deepEqual(mainClocks(ctrl), ['0:00:45', '0:01:20']);
  assert.equal(text(renderClock(chapter, 'white', true)), '0:45');

  study.socketHandler('clock', { p, c: 6_000, relayClocks: [6_000, null] });
  assert.equal(chapter.clockAt, undefined);
  assert.equal(chapter.players!.black.clock, undefined);
  assert.deepEqual(mainClocks(ctrl), ['0:00:45', '0:01:20']);

  study.socketHandler('clock', { p, c: 6_000, relayClocks: [6_000, null], clockAge: 0 });
  assert.equal(chapter.clockAt, now);
  t.mock.timers.tick(3_000);
  assert.equal(mainClocks(ctrl)[0], '0:00:57');
  assert.equal(text(renderClock(chapter, 'white', true)), '0:57');
  study.socketHandler('clock', { p, c: 6_000, relayClocks: [6_000, null] });
  assert.equal(chapter.clockAt, now, 'an absent age leaves the existing origin unchanged');
  assert.equal(mainClocks(ctrl)[0], '0:00:57');

  study.socketHandler('clock', { p, clockAge: -5_000 });
  assert.equal(chapter.clockAt, now + 8_000);
  assert.equal(tree.nodeAtPath('aa').clock, 6_000);
  assert.equal(chapter.players!.white.clock, 6_000);
  assert.equal(mainClocks(ctrl)[0], '0:01:00');
  assert.equal(text(renderClock(chapter, 'white', true)), '1:00');
  t.mock.timers.tick(6_000);
  assert.equal(mainClocks(ctrl)[0], '0:00:59');
  assert.equal(text(renderClock(chapter, 'white', true)), '0:59');

  study.socketHandler('clock', { p, c: null, clockAge: null });
  assert.equal(chapter.clockAt, undefined);
  assert.equal(tree.nodeAtPath('aa').clock, 6_000);
  assert.equal(mainClocks(ctrl)[0], '0:00:36');
  assert.equal(text(renderClock(chapter, 'white', true)), '0:36');
  study.socketHandler('clock', { p, c: 0, relayClocks: [0, null] });
  assert.equal(tree.nodeAtPath('aa').clock, 0);
  assert.equal(chapter.players!.white.clock, 0);
  assert.equal(chapter.players!.black.clock, undefined);
  assert.equal(mainClocks(ctrl)[0], '0:00:00');
  assert.equal(text(renderClock(chapter, 'white', true)), '0:00');

  study.socketHandler('clock', { p, c: null });
  assert.equal(tree.nodeAtPath('aa').clock, undefined, 'legacy clock clearing is preserved');
});

test('relay nodes update the active tree and preview with and without a clock age', t => {
  t.mock.timers.enable({ apis: ['Date', 'setInterval'], now });
  const { study, ctrl } = fixture();
  const chapter = study.currentChapter();
  study.socketHandler('addNode', {
    p: { chapterId: chapter.id, path: 'aabb' },
    n: { id: 'cc', ply: 3, fen: nextBlackFen, uci: 'g1f3', clock: 5_200 },
    s: false,
    relayPath: '!',
    clockAge: 8_000,
  });
  assert.equal(chapter.clockAt, now - 8_000);
  assert.equal(study.data.chapter.relayPath, 'aabbcc');
  assert.equal(ctrl.path, 'aabbcc');
  assert.deepEqual(mainClocks(ctrl, 'aabbcc'), ['0:00:52', '0:01:12']);
  assert.equal(text(renderClock(chapter, 'black', true)), '1:12');

  t.mock.timers.tick(2_000);
  study.socketHandler('addNode', {
    p: { chapterId: chapter.id, path: 'aabbcc' },
    n: { id: 'dd', ply: 4, fen: whiteFen, uci: 'b8c6', clock: 7_600 },
    s: false,
    relayPath: 'aabbccdd',
  });
  assert.equal(chapter.clockAt, undefined);
  assert.equal(chapter.lastMoveAt, now + 2_000);
  assert.equal(ctrl.path, 'aabbccdd');
  assert.deepEqual(mainClocks(ctrl, 'aabbccdd'), ['0:00:52', '0:01:16']);
  assert.equal(text(renderClock(chapter, 'white', true)), '0:52');
});

test('other chapters receive clock and relay-node updates, while variations leave the origin alone', t => {
  t.mock.timers.enable({ apis: ['Date', 'setInterval'], now });
  const { study, tree, redraw } = fixture();
  const other = study.chapters.list.get('chapterB')!;
  const oldMainClock = tree.nodeAtPath('aa').clock;
  let draws = redraw.mock.callCount();
  study.socketHandler('addNode', {
    p: { chapterId: other.id, path: 'aabb' },
    n: { id: 'cc', ply: 3, fen: nextBlackFen, uci: 'g1f3', clock: 5_200 },
    s: false,
    relayPath: '!',
    clockAge: 8_000,
  });
  assert.ok(redraw.mock.callCount() > draws);
  assert.equal(other.fen, nextBlackFen);
  assert.equal(other.lastMoveAt, now);
  assert.equal(other.clockAt, now - 8_000);
  assert.equal(other.players!.white.clock, 5_200);
  assert.equal(text(renderClock(other, 'black', true)), '1:12');
  assert.equal(tree.nodeAtPath('aa').clock, oldMainClock);

  t.mock.timers.tick(2_000);
  study.socketHandler('addNode', {
    p: { chapterId: other.id, path: 'aabb' },
    n: { id: 'dd', ply: 3, fen: blackFen, uci: 'd2d4', clock: 1_000 },
    s: false,
    relayPath: 'aabbcc',
    clockAge: 500,
  });
  assert.equal(other.clockAt, now - 8_000);
  assert.equal(other.lastMoveAt, now);
  assert.equal(other.fen, nextBlackFen);

  study.socketHandler('addNode', {
    p: { chapterId: other.id, path: 'aabbcc' },
    n: { id: 'ee', ply: 4, fen: whiteFen, uci: 'b8c6', clock: 7_600 },
    s: false,
    relayPath: 'aabbccee',
  });
  assert.equal(other.clockAt, undefined);
  assert.equal(other.lastMoveAt, now + 2_000);
  assert.equal(other.players!.black.clock, 7_600);
  assert.equal(text(renderClock(other, 'white', true)), '0:52');

  draws = redraw.mock.callCount();
  study.socketHandler('clock', {
    p: { chapterId: other.id, path: 'aabbcc' },
    c: 5_000,
    relayClocks: [5_000, null],
    clockAge: 6_000,
  });
  assert.ok(redraw.mock.callCount() > draws);
  assert.equal(other.clockAt, now - 4_000);
  assert.equal(other.players!.white.clock, 5_000);
  assert.equal(other.players!.black.clock, undefined);
  assert.equal(text(renderClock(other, 'white', true)), '0:44');
  assert.equal(tree.nodeAtPath('aa').clock, oldMainClock);

  draws = redraw.mock.callCount();
  study.socketHandler('clock', { p: { chapterId: other.id, path: 'aabbcc' }, clockAge: -2_000 });
  assert.ok(redraw.mock.callCount() > draws);
  assert.equal(other.clockAt, now + 4_000);
  assert.equal(other.players!.white.clock, 5_000);
  assert.equal(text(renderClock(other, 'white', true)), '0:50');
  assert.equal(tree.nodeAtPath('aa').clock, oldMainClock);
});

test('result tags stop current and background clocks, including an XHR without previews', t => {
  t.mock.timers.enable({ apis: ['Date', 'setInterval'], now });
  const { study, ctrl, data, redraw } = fixture();
  const other = study.chapters.list.get('chapterB')!;
  const draws = redraw.mock.callCount();
  study.socketHandler('setTags', { chapterId: other.id, tags: [['Result', '1/2-1/2']] });
  assert.ok(redraw.mock.callCount() > draws);
  assert.equal(other.status, '½-½');
  const otherClock = renderClock(other, 'white', true)!;
  assert.equal(text(otherClock), '1:00');
  assert.equal(otherClock.data?.class?.['clock--run'], false);
  assert.deepEqual(mainClocks(ctrl), ['0:00:40', '0:01:20']);

  study.socketHandler('setTags', { chapterId: 'chapterA', tags: [['Result', '1-0']] });
  const currentClock = renderClock(study.currentChapter(), 'white', true)!;
  assert.equal(currentClock.data?.class?.['clock--run'], false);
  assert.equal(text(currentClock), '1:00');
  assert.deepEqual(mainClocks(ctrl), ['0:01:00', '0:01:20']);
  t.mock.timers.tick(10_000);
  assert.deepEqual(mainClocks(ctrl), ['0:01:00', '0:01:20']);
  assert.equal(text(renderClock(other, 'white', true)), '1:00');

  study.socketHandler('setTags', { chapterId: 'chapterA', tags: [] });
  assert.equal(study.currentChapter().status, undefined);
  assert.equal(mainClocks(ctrl)[0], '0:00:30');
  assert.equal(text(renderClock(study.currentChapter(), 'white', true)), '0:30');
  study.socketHandler('setTags', { chapterId: other.id, tags: [] });
  assert.equal(other.status, undefined);
  assert.equal(text(renderClock(other, 'white', true)), '0:30');
  study.socketHandler('setTags', { chapterId: 'chapterA', tags: [['Result', '*']] });
  assert.equal(study.currentChapter().status, '*');
  assert.equal(mainClocks(ctrl)[0], '0:00:30');
  reload(study, data, [['Result', '0-1']], -3_000);
  assert.equal(study.currentChapter().status, '0-1');
  assert.equal(study.currentChapter().clockAt, now - 20_000);
  assert.equal(study.relayClockAt, now + 13_000);
  assert.deepEqual(mainClocks(ctrl), ['0:01:00', '0:01:20']);
  assert.equal(renderClock(study.currentChapter(), 'white', true)!.data?.class?.['clock--run'], false);
});

test('only the side to move ticks and future references cannot increase either clock', t => {
  t.mock.timers.enable({ apis: ['Date', 'setInterval'], now });
  const { study, ctrl, tree } = fixture({ age: -5_000 });
  assert.deepEqual(mainClocks(ctrl, 'aa'), ['0:01:00', '0:01:30'], 'past positions do not tick');
  const chapter = study.currentChapter();
  chapter.fen = blackFen;
  chapter.lastMove = 'e2e4';
  chapter.players!.black.clock = 9_000;
  study.data.chapter.relayPath = 'aa';
  ctrl.path = 'aa';
  ctrl.node = tree.nodeAtPath('aa');
  assert.deepEqual(mainClocks(ctrl, 'aa'), ['0:01:00', '0:01:30']);
  assert.equal(text(renderClock(chapter, 'white', true)), '1:00');
  assert.equal(text(renderClock(chapter, 'black', true)), '1:30');
  t.mock.timers.tick(7_000);
  assert.deepEqual(mainClocks(ctrl, 'aa'), ['0:01:00', '0:01:28']);
  assert.equal(text(renderClock(chapter, 'black', true)), '1:28');
  tree.root.clock = 0;
  chapter.players!.black.clock = 0;
  assert.equal(mainClocks(ctrl, 'aa')[1], '0:00:00');
  assert.equal(text(renderClock(chapter, 'black', true)), '0:00');
  chapter.players!.black.clock = undefined;
  assert.equal(renderClock(chapter, 'black', true), undefined);
});

test('a relay reference at the Unix epoch is still a valid origin', t => {
  t.mock.timers.enable({ apis: ['Date', 'setInterval'], now });
  const { study, ctrl } = fixture();
  const chapter = study.currentChapter();
  study.socketHandler('clock', {
    p: { chapterId: chapter.id, path: 'aa' },
    c: 200_000,
    relayClocks: [200_000, null],
    clockAge: now,
  });
  assert.equal(chapter.clockAt, 0);
  assert.equal(mainClocks(ctrl)[0], '0:16:40');
  assert.equal(text(renderClock(chapter, 'white', true)), '16:40');
});

test('non-relay studies and autoplay keep their previous clock behaviour', t => {
  t.mock.timers.enable({ apis: ['Date', 'setInterval'], now });
  const { study, ctrl } = fixture({ relay: false, thinkTime: 15 });
  const chapter = study.currentChapter();
  assert.equal(chapter.clockAt, undefined);
  assert.deepEqual(mainClocks(ctrl), ['01:00', '01:20']);
  chapter.clockAt = now + 20_000;
  assert.equal(text(renderClock(chapter, 'white')), '0:45');
  study.socketHandler('clock', { p: { chapterId: chapter.id, path: 'aa' }, c: 5_000, clockAge: 0 });
  assert.equal(ctrl.tree.nodeAtPath('aa').clock, 5_000);
  assert.equal(chapter.clockAt, now + 20_000);
  assert.deepEqual(mainClocks(ctrl), ['00:50.0', '01:20']);

  ctrl.study = undefined;
  ctrl.autoplay.lastMoveAt = now - 10_000;
  assert.deepEqual(mainClocks(ctrl), ['00:40.0', '01:20']);
  ctrl.autoplay.lastMoveAt = now + 5_000;
  assert.deepEqual(mainClocks(ctrl), ['00:55.0', '01:20']);
  ctrl.autoplay.lastMoveAt = 0;
  assert.deepEqual(mainClocks(ctrl), ['00:50.0', '01:20']);
});
