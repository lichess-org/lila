import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import studyView = require('./study/studyView');
import { nodeFullName, bind } from './util';
import AnalyseController from './ctrl';
import { patch } from './main';

export interface Opts {
  path: Tree.Path;
  root: AnalyseController;
}

interface Coords {
  x: number;
  y: number;
}

const elementId = 'analyse-cm';

function getPosition(e: MouseEvent): Coords {
  let posx = 0;
  let posy = 0;
  if (e.pageX || e.pageY) {
    posx = e.pageX;
    posy = e.pageY;
  } else if (e.clientX || e.clientY) {
    posx = e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
    posy = e.clientY + document.body.scrollTop + document.documentElement.scrollTop;
  }
  return {
    x: posx,
    y: posy
  }
}

function positionMenu(menu: HTMLElement, coords: Coords): void {

  const menuWidth = menu.offsetWidth + 4,
  menuHeight = menu.offsetHeight + 4,
  windowWidth = window.innerWidth,
  windowHeight = window.innerHeight;

  menu.style.left = (windowWidth - coords.x) < menuWidth ?
    windowWidth - menuWidth + "px" :
    menu.style.left = coords.x + "px";

  menu.style.top = (windowHeight - coords.y) < menuHeight ?
    windowHeight - menuHeight + "px" :
    menu.style.top = coords.y + "px";
}

function ctrl(opts: Opts) {
  return {
    path: opts.path,
    node: opts.root.tree.nodeAtPath(opts.path),
    isMainline: opts.root.onMainline,
    root: opts.root
  };
}

function action(icon: string, text: string, handler: () => void): VNode {
  return h('a.action', {
    attrs: { 'data-icon': icon },
    hook: bind('click', handler)
  }, text);
}

function view(ctrl, coords: Coords): VNode {
  return h('div#' + elementId + '.visible', {
    hook: {
      insert: vnode => positionMenu(vnode.elm as HTMLElement, coords),
      postpatch: (_, vnode) => positionMenu(vnode.elm as HTMLElement, coords)
    }
  }, [
    h('p.title', nodeFullName(ctrl.node)),
    ctrl.isMainline ? null : action('S', 'Promote variation', () => ctrl.root.promote(ctrl.path, false)),
    ctrl.isMainline ? null : action('E', 'Make main line', () => ctrl.root.promote(ctrl.path, true)),
    action('q', 'Delete from here', () => ctrl.root.deleteNode(ctrl.path))
  ].concat(
    ctrl.root.study ? studyView.contextMenu(ctrl.root.study, ctrl.path, ctrl.node) : []
  ));
}

export default function(e: MouseEvent, opts: Opts): void {
  const el = document.getElementById(elementId) ||
    $('<div id="' + elementId + '">').appendTo($('body'))[0];
  opts.root.contextMenuPath = opts.path;
  function close() {
    opts.root.contextMenuPath = undefined;
    document.removeEventListener("click", close, false);
    el.classList.remove('visible');
    opts.root.redraw();
  };
  document.addEventListener("click", close, false);
  el.innerHTML = '';
  patch(el, view(ctrl(opts), getPosition(e)));
}
