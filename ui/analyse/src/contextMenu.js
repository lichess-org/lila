var m = require('mithril');
var plyToTurn = require('./tree/ops').plyToTurn;
var partial = require('chessground').util.partial;

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

  if ((windowWidth - coords.x) < menuWidth) {
    menu.style.left = windowWidth - menuWidth + "px";
  } else {
    menu.style.left = coords.x + "px";
  }

  if ((windowHeight - coords.y) < menuHeight) {
    menu.style.top = windowHeight - menuHeight + "px";
  } else {
    menu.style.top = coords.y + "px";
  }
}

function ctrl(opts) {
  return {
    path: opts.path,
    node: opts.root.tree.nodeAtPath(opts.path),
    isMainline: opts.root.tree.pathIsMainline(opts.path),
    root: opts.root,
    submit: function(d) {
      opts.close();
      opts.submit(d);
    }
  };
}

function view(ctrl) {
  var action = function(icon, text, handler) {
    return m('a.action', {
      'data-icon': icon,
      onclick: handler
    }, text);
  }
  return m('div', {
    config: function(el, isUpdate, ctx) {
      if (isUpdate) return;
    }
  }, [
    m('p.title', [
      plyToTurn(ctrl.node.ply),
      ctrl.node.ply % 2 === 1 ? '. ' : '... ',
      ctrl.node.san
    ]),
    ctrl.isMainline ? null : action('E', 'Promote to main line', partial(ctrl.root.promoteNode, ctrl.path)),
    action('q', 'Delete from here', partial(ctrl.root.deleteNode, ctrl.path))
  ]);
}

module.exports = function(e, opts) {
  var el = document.getElementById(elementId) ||
    $('<div id="' + elementId + '">').appendTo($('body'))[0];
  opts.close = function() {
    document.removeEventListener("click", opts.close, false);
    el.classList.remove('visible');
    m.render(el, null);
  };
  document.addEventListener("click", opts.close, false);
  m.render(el, view(ctrl(opts)));
  positionMenu(el, getPosition(e));
  el.classList.add('visible');
};
