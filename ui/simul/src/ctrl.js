var socket = require('./socket');
var simul = require('./simul');

module.exports = function(env) {

  this.data = env.data;
  this.arbiterData = undefined;
  this.evals = env.data.evals;
  this.pref = env.data.pref;

  this.toggleCandidates = false;
  this.toggleArbiter = false;
  this.userId = env.userId;

  this.arbiterSort = 'assessment';
  this.arbiterPlayingOnly = false;
  this.arbiterSortDescending = true;
  this.arbiterSortTarget = undefined;
  this.toggleArbiterSort = function(target, prop) {
    if (this.arbiterSort === prop) {
      if (this.arbiterSortDescending) {
        this.arbiterSortDescending = false;
        target.setAttribute('data-icon', 'R');
      } else {
        this.arbiterSort = 'assessment';
        this.arbiterSortDescending = true;
        target.setAttribute('data-icon', '');
      }
    } else {
      this.arbiterSort = prop;
      this.arbiterSortDescending = true;
      if (this.arbiterSortTarget)
        this.arbiterSortTarget.setAttribute('data-icon', '');
      target.setAttribute('data-icon', 'S');
      this.arbiterSortTarget = target;
    }
  }
  this.toggleArbiterPlaying = function(value) {
    this.arbiterPlayingOnly = value;
  }

  this.socket = new socket(env.socketSend, this);

  this.reload = function(data) {
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
    lidraughts.storage.set('lidraughts.move_on', '1'); // hideous hack :D

  this.trans = lidraughts.trans(env.i18n);
};
