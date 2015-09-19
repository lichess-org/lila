// #FIXME jQuery crap here

module.exports = function(el, ctrl) {
  var url = ctrl.data.player.spectator ?
    router.game(ctrl.data, ctrl.data.player.color) :
    router.player(ctrl.data);
  url += '/text';
  $(el).load(url);
};
