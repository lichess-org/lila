var socket = require('./socket');
var simul = require('./simul');
var text = require('./text');

module.exports = function(env) {

  this.env = env;

  this.data = env.data;

  this.userId = env.userId;

  this.socket = new socket(env.socketSend, this);
  this.text = text.ctrl();

  this.reload = function(data) {
    data.team = this.data.simul; // reload data does not contain the simul anymore
    this.data = data;
    startWatching();
  }.bind(this);

  var alreadyWatching = [];
  var startWatching = function() {
    var newIds = this.data.pairings.map(function(p) {
      return p.game.id;
    }).filter(function(id) {
      return !alreadyWatching.includes(id);
    });
    if (newIds.length) {
      setTimeout(function() {
        this.socket.send("startWatching", newIds.join(' '));
      }.bind(this), 1000);
      newIds.forEach(alreadyWatching.push.bind(alreadyWatching));
    }
  }.bind(this);
  startWatching();

  if (simul.createdByMe(this) && this.data.isCreated)
    lichess.storage.set('lichess.move_on', '1'); // hideous hack :D

  this.trans = lichess.trans(env.i18n);

  this.teamBlock = this.data.team && !this.data.team.isIn;
};
