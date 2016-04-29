var m = require('mithril');
var partial = require('chessground').util.partial;
var studyView = require('./study/studyView');
var nodeFullName = require('./util').nodeFullName;

var elementId = 'analyse-cm';

function getPosition(e) {
  var posx = 0;
  var posy = 0;
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

function positionMenu(menu, coords) {

  var menuWidth = menu.offsetWidth + 4;
  var menuHeight = menu.offsetHeight + 4;

  var windowWidth = window.innerWidth;
  var windowHeight = window.innerHeight;

  if ((windowWidth - coords.x) < menuWidth)
    menu.style.left = windowWidth - menuWidth + "px";
  else
    menu.style.left = coords.x + "px";

  if ((windowHeight - coords.y) < menuHeight)
    menu.style.top = windowHeight - menuHeight + "px";
  else
    menu.style.top = coords.y + "px";

}

function ctrl(opts) {
  return {
    path: opts.path,
    node: opts.root.tree.nodeAtPath(opts.path),
    isMainline: opts.root.tree.pathIsMainline(opts.path),
    root: opts.root
  };
}

function action(icon, text, handler) {
  return m('a.action', {
    'data-icon': icon,
    onclick: handler
  }, text);
}

function view(ctrl) {
  return m('div', {
    config: function(el, isUpdate, ctx) {
      if (isUpdate) return;
    }
  }, [
    m('p.title', nodeFullName(ctrl.node)),
    ctrl.isMainline ? null : action('E', 'Promote to main line', partial(ctrl.root.promoteNode, ctrl.path)),
    action('q', 'Delete from here', partial(ctrl.root.deleteNode, ctrl.path)),
    ctrl.root.study ? studyView.contextMenu(ctrl.root.study, ctrl.path, ctrl.node) : null
  ]);
}

module.exports = {
  open: function(e, opts) {
    var el = document.getElementById(elementId) ||
      $('<div id="' + elementId + '">').appendTo($('body'))[0];
    opts.root.vm.contextMenuPath = opts.path;
    opts.close = function() {
      opts.root.vm.contextMenuPath = null;
      document.removeEventListener("click", opts.close, false);
      el.classList.remove('visible');
      m.render(el, null);
      m.redraw();
    };
    document.addEventListener("click", opts.close, false);
    positionMenu(el, getPosition(e));
    m.render(el, view(ctrl(opts)));
    el.classList.add('visible');
  }
};
