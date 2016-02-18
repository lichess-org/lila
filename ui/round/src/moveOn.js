var game = require('game').game;
var xhr = require('./xhr');
var router = require('game').router;
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
  };

  this.next = function(force) {
    if (!this.value || ctrl.data.player.spectator || !game.isSwitchable(ctrl.data) || game.isPlayerTurn(ctrl.data)) return;
    if (force) redirect('/round-next/' + ctrl.data.game.id);
    else if (ctrl.data.simul) {
      if (ctrl.data.simul.hostId === ctrl.userId && ctrl.data.simul.nbPlaying > 1)
        redirect('/round-next/' + ctrl.data.game.id);
    } else xhr.whatsNext(ctrl).then(function(data) {
      if (data.next) redirect('/' + data.next);
    }.bind(this));
  }.bind(this);
};
