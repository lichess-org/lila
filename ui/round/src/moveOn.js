var game = require('game').game;
var xhr = require('./xhr');
var m = require('mithril');

module.exports = function(ctrl, key) {

  this.value = lichess.storage.get(key) === '1';

  var store = function() {
    lichess.storage.set(key, this.value ? '1' : '0');
  }.bind(this);

  this.toggle = function() {
    this.value = !this.value;
    store();
    this.next();
    return this.value;
  }.bind(this);

  this.get = function() {
    return this.value;
  }.bind(this);

  this.set = function(v) {
    this.value = v;
    store();
  }.bind(this);

  this.next = function() {
    if (!this.value || ctrl.data.player.spectator || ctrl.data.game.tournamentId || game.isPlayerTurn(ctrl.data)) return;
    ctrl.vm.redirecting = true;
    lichess.hasToReload = true;
    location.href = ctrl.router.Round.next(ctrl.data.game.id).url;
    m.redraw();
  }.bind(this);
};
