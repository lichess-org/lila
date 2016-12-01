var m = require('mithril');
var xhr = require('./xhr');
var hookRepo = require('./hookRepo');
var partial = require('chessground').util.partial;

module.exports = function(send, ctrl) {

  this.send = send;

  var handlers = {
    had: function(hook) {
      hookRepo.add(ctrl, hook);
      if (hook.action === 'cancel') ctrl.flushHooks(true);
      if (ctrl.vm.tab === 'real_time') m.redraw();
    },
    hrm: function(id) {
      hookRepo.remove(ctrl, id);
      if (ctrl.vm.tab === 'real_time') m.redraw();
    },
    hooks: function(hooks) {
      hookRepo.setAll(ctrl, hooks);
      ctrl.flushHooks(true);
      m.redraw();
    },
    hli: function(ids) {
      hookRepo.syncIds(ctrl, ids.split(','));
      if (ctrl.vm.tab === 'real_time') m.redraw();
    },
    reload_seeks: function() {
      if (ctrl.vm.tab === 'seeks') xhr.seeks().then(ctrl.setSeeks);
    },
    nb_hooks: function(nb) {
      ctrl.nbHooks = nb;
      m.redraw();
    }
  };

  this.realTimeIn = function() {
    send('hookIn');
  };
  this.realTimeOut = function() {
    send('hookOut');
  };

  this.poolIn = function(id) {
    send('poolIn', id);
  };

  this.poolOut = function(id) {
    send('poolOut', id);
  };

  this.receive = function(type, data) {
    if (this.music) this.music.receive(type, data);
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);

  lichess.idleTimer(5 * 60 * 1000, partial(send, 'idle', true), function() {
    location.reload();
  });

  this.music = null;
  lichess.pubsub.on('sound_set', function(set) {
    if (!this.music && set === 'music')
      lichess.loadScript('/assets/javascripts/music/lobby.js').then(function() {
        this.music = lichessLobbyMusic();
        ctrl.setMode('chart');
      }.bind(this));
    if (this.music && set !== 'music') this.music = null;
  }.bind(this));
};
