var m = require('mithril');

var throttle = require('coach').throttle;

module.exports = function(opts) {

  this.user = opts.user;
  this.color = opts.color;
  this.nbPeriods = opts.nbPeriods;

  this.vm = {
    preloading: !!this.nbPeriods,
    loading: true,
    range: [0, this.nbPeriods],
    inspecting: 'global'
  };

  var requestData = throttle(1000, false, function() {
    m.request({
      url: '/coach/move/' + this.user.id + '.json',
      data: {
        range: this.vm.range.join('-')
      }
    }).then(function(data) {
      console.log(data);
      this.data = data;
      if (location.hash) this.inspect(location.hash.replace(/#/, '').replace(/_/g, ' '));
      this.vm.preloading = false;
      this.vm.loading = false;
    }.bind(this));
    m.redraw();
  }.bind(this));
  if (this.nbPeriods) setTimeout(requestData, 200);

  this.selectPeriodRange = function(from, to) {
    this.vm.range = [from, to];
    this.vm.loading = true;
    if (from === to) this.data = null;
    else requestData();
    m.redraw();
  }.bind(this);

  this.jumpBy = function(delta) {
    if (!this.vm.inspecting) return;
    var keys = this.data.perfs.map(function(o) {
      return o.perf.key
    });
    var i = keys.indexOf(this.vm.inspecting);
    var i2 = (i + delta) % keys.length;
    if (i2 < 0) i2 = keys.length - 1;
    this.inspect(keys[i2]);
  }.bind(this);

  this.inspect = function(key) {
    if (!this.data || !this.data.perfs.some(function(o) {
      return o.perf.key === key;
    })) return;
    if (this.vm.inspecting === key) return;
    if (window.history.replaceState)
      window.history.replaceState(null, null, '#' + key);
    this.vm.inspecting = key;
  }.bind(this);

  this.trans = lichess.trans(opts.i18n);
};
