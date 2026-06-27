import assert from 'node:assert/strict';
import { test } from 'node:test';

function ctrlStub() {
  return {
    members: { canContribute: () => true },
    chapters: {
      list: {
        size: () => 2,
        all: () => [
          { id: 'chapter-a', name: 'Chapter A' },
          { id: 'chapter-b', name: 'Chapter B' },
        ],
      },
      scroller: {
        request: () => {},
        scrollIfNeeded: () => {},
      },
      sort: () => {},
      editForm: {
        isEditing: () => false,
        toggle: () => {},
      },
      toggleNewForm: () => {},
    },
    currentChapter: () => ({ id: 'chapter-a' }),
    vm: { loading: false },
    setChapter: () => {},
    redraw: () => {},
  } as any;
}

async function studyListVNode() {
  document.documentElement.lang = 'en-US';
  installIndexedDbStub();
  const { view } = await import('../src/study/studyChapters');
  const vnode = (view(ctrlStub()) as any).children[0];
  vnode.elm = document.createElement('div');
  return vnode;
}

function installIndexedDbStub() {
  const request: any = {
    result: {
      objectStoreNames: { contains: () => true },
      transaction: () => ({ objectStore: () => ({}) }),
      close: () => {},
    },
  };
  Object.defineProperty(window, 'indexedDB', {
    value: {
      open: () => {
        setTimeout(() => request.onsuccess?.({ target: request }), 0);
        return request;
      },
      deleteDatabase: () => {},
    },
    configurable: true,
  });
}

function deferSortableLoad() {
  const resolvers: ((module: any) => void)[] = [];
  let loadCalls = 0;
  let createCalls = 0;

  (site as any).asset = {
    loadEsm: () => {
      loadCalls++;
      return new Promise(resolve => resolvers.push(resolve));
    },
  };

  const sortableModule = {
    create: () => {
      createCalls++;
      return {
        toArray: () => [],
        destroy: () => {},
      };
    },
  };

  return {
    get loadCalls() {
      return loadCalls;
    },
    get createCalls() {
      return createCalls;
    },
    resolveAll: () => resolvers.splice(0).forEach(resolve => resolve(sortableModule)),
  };
}

test('study chapter list queues one Sortable initializer while the asset is loading', async () => {
  const loader = deferSortableLoad();
  const vnode = await studyListVNode();

  vnode.data.hook.insert(vnode);
  vnode.data.hook.postpatch(vnode, vnode);

  assert.equal(loader.loadCalls, 1);

  loader.resolveAll();
  await new Promise(resolve => setTimeout(resolve, 0));

  assert.equal(loader.createCalls, 1);
});

test('study chapter list skips Sortable creation after the vnode is destroyed', async () => {
  const loader = deferSortableLoad();
  const vnode = await studyListVNode();

  vnode.data.hook.insert(vnode);
  vnode.data.hook.destroy(vnode);

  loader.resolveAll();
  await new Promise(resolve => setTimeout(resolve, 0));

  assert.equal(loader.createCalls, 0);
});
