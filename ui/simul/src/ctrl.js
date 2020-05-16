var socket = require('./socket');
var simul = require('./simul');
var text = require('./text');
var xhr = require('./xhr');

module.exports = function(env) {

  this.env = env;

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
  this.text = text.ctrl();

  this.reload = function(data) {
    if (this.data.isCreated && !data.isCreated) {
      // hack to change parent class - remove me when moving to snabbdom
      $('main.simul-created').removeClass('simul-created');
    }
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
    lidraughts.storage.set('lidraughts.move_on', '1'); // hideous hack :D

  this.trans = lidraughts.trans(env.i18n);

  this.teamBlock = this.data.team && !this.data.team.isIn;

  this.hostPing = () => {
    if (simul.createdByMe(this) && this.data.isCreated) {
      xhr.ping(this);
      setTimeout(this.hostPing, 10000);
    }
  };
  this.hostPing();
};
