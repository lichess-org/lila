var xhr = require('./xhr');
var m = require('mithril');

module.exports = function(env) {

  this.data = [];

  this.vm = {
    initiating: true,
    reloading: false
  };

  this.setNotifications = function(data) {
    this.vm.initiating = false;
    this.vm.reloading = false;
    this.data = data;

    console.dir(data);

    m.redraw();
  }.bind(this);

  this.updateNotifications = function() {
      this.vm.reloading = true;
      return xhr.load().then(this.setNotifications);
  }.bind(this);

  this.markAllReadServer = function() {
    xhr.markAllRead();
  }.bind(this);

  this.updateNotifications();
};
