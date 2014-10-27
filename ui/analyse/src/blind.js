// #FIXME jQuery crap here

module.exports = function(el, ctrl) {
  var route = ctrl.data.player.spectator ? ctrl.router.Round.watcherText(ctrl.data.game.id, ctrl.data.player.color) : ctrl.router.Round.playerText(ctrl.data.game.id + ctrl.data.player.id);
  $(el).load(route.url);
};
