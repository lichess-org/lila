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
    this.next(true);
    return this.value;
  }.bind(this);

  this.get = function() {
    return this.value;
  }.bind(this);

  this.set = function(v) {
    this.value = v;
    store();
  }.bind(this);

  var redirect = function(href) {
    ctrl.setRedirecting();
    lichess.hasToReload = true;
    location.href = href;
    m.redraw();
  };

  this.next = function(force) {
    if (!this.value || ctrl.data.player.spectator || ctrl.data.game.tournamentId || game.isPlayerTurn(ctrl.data)) return;
    if (force) redirect(ctrl.router.Round.next(ctrl.data.game.id).url);
    else if (ctrl.data.simul) {
      if (ctrl.data.simul.hostId === ctrl.userId && ctrl.data.simul.nbPlaying > 1)
        redirect(ctrl.router.Round.next(ctrl.data.game.id).url)
    } else xhr.whatsNext(ctrl).then(function(data) {
      if (data.next) redirect('/' + data.next);
    }.bind(this));
  }.bind(this);
};
