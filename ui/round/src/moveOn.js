var game = require('game').game;
var xhr = require('./xhr');
var m = require('mithril');

module.exports = function(ctrl, key) {

  this.value = storage.get(key) === '1';

  var store = function() {
    storage.set(key, this.value ? '1' : '0');
  }.bind(this);

  this.toggle = function() {
    this.value = !this.value;
    store();
    this.next(ctrl);
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
    if (!this.value || !game.isPlayerPlaying(ctrl.data) || game.isPlayerTurn(ctrl.data)) return;
    xhr.next(ctrl).then(function(data) {
      if (data.next && this.value) {
        ctrl.vm.redirecting = true;
        lichess.hasToReload = true;
        m.redraw();
        location.href = '/' + data.next;
      }
    }.bind(this));
  }.bind(this);
};
