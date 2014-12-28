function order(a, b) {
  return a.rating > b.rating ? -1 : 1;
}

function sort(ctrl) {
  ctrl.data.seeks.sort(order);
}

function fixBC(seek) {
  seek.mode = seek.mode === 'Casual' ? 0 : 1;
}

function init(ctrl, seek) {
  seek.action = (ctrl.data.me && seek.username === ctrl.data.me.username) ? 'cancelSeek' : 'joinSeek';
  fixBC(seek);
}

module.exports = {
  init: init,
  sort: sort,
  find: function(ctrl, id) {
    return ctrl.data.seeks.filter(function(s) {
      return s.id === id;
    })[0];
  }
};
