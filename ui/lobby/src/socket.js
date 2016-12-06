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
    }
  };

  this.realTimeIn = function() {
    send('hookIn');
  };
  this.realTimeOut = function() {
    send('hookOut');
  };

  this.poolIn = function(member) {
    // last arg=true: must not retry
    // because if poolIn is sent before socket opens,
    // then poolOut is sent,
    // then poolIn shouldn't be sent again after socket opens.
    // poolIn is sent anyway on socket open event.
    send('poolIn', member, {}, true);
  };

  this.poolOut = function(member) {
    send('poolOut', member.id);
  };

  this.receive = function(type, data) {
    if (this.music) this.music.receive(type, data);
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);

  lichess.idleTimer(3 * 60 * 1000, partial(send, 'idle', true), function() {
    send('idle', false);
    ctrl.awake();
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
