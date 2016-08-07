var k = Mousetrap;
var m = require('mithril');
var partial = require('chessground').util.partial;

function findOnlyOrig(allDests, dest) {
  var found;
  for (var key in allDests)
    if (allDests[key].indexOf(dest) > -1) {
      if (found) return;
      found = key;
    }
  return found;
}

module.exports = {
  ctrl: function(root) {
    var cg = root.chessground;
    var focus = m.prop(false);
    return {
      focus: focus,
      select: function(key) {
        var selected = cg.data.selected;
        if (selected === key) return cg.cancelMove();
        var onlyOrig = selected ? null : findOnlyOrig(cg.data.movable.dests, key);
        if (onlyOrig) cg.selectSquare(onlyOrig);
        cg.selectSquare(key);
      }
    };
  },
  view: function(ctrl) {
    return m('div.keyboard-move', [
      m('input', {
        config: function(el, isUpdate) {
          if (!isUpdate) {
            el.focus();
            k.bind('enter', function() {
              el.focus();
            });
          }
        },
        onfocus: partial(ctrl.focus, true),
        onblur: partial(ctrl.focus, false),
        onkeyup: function(e) {
          var v = e.target.value;
          if (v.length < 2) return;
          if (v.match(/[a-h][1-8]/)) ctrl.select(v);
          e.target.value = '';
        }
      }),
      ctrl.focus() ?
      m('em', 'Enter coordinates to select squares (like e4 or g8)') :
      m('strong', 'Press <enter> to focus')
    ]);
  }
};
