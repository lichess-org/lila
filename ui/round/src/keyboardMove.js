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
    var focus = false;
    return {
      getFocus: function() {
        return focus;
      },
      setFocus: function(v) {
        focus = v;
      },
      select: function(key) {
        var selected = cg.data.selected;
        if (selected === key) return cg.cancelMove();
        var onlyOrig = selected ? null : findOnlyOrig(cg.data.movable.dests, key);
        if (onlyOrig) cg.selectSquare(onlyOrig);
        cg.selectSquare(key);
      },
      cancel: cg.cancelMove
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
        onfocus: partial(ctrl.setFocus, true),
        onblur: partial(ctrl.setFocus, false),
        onkeyup: function(e) {
          if (e.which == 27) {
            e.target.value = '';
            return ctrl.cancel();
          }
          var v = e.target.value;
          if (v.indexOf('/') > -1) {
            var chatInput = document.querySelector('.mchat input.lichess_say');
            if (chatInput) chatInput.focus();
          } else {
            if (v.length < 2) return;
            if (v.match(/[a-h][1-8]/)) ctrl.select(v);
          }
          e.target.value = '';
        }
      }),
      ctrl.getFocus() ?
      m('em', 'Enter coordinates to select squares. Press escape to cancel, press / to focus chat') :
      m('strong', 'Press <enter> to focus')
    ]);
  }
};
