var m = require('mithril');
var throttle = require('./throttle');

module.exports = function(env) {

  this.ui = env.ui;
  this.userId = env.userId;

  this.vm = {
    loading: true,
    metric: env.ui.metrics[0].key,
    dimension: 'opponentStrength'
  };

  var requestData = throttle(1000, false, function() {
    this.vm.loading = true;
    m.request({
      method: 'post',
      url: '/coach/data/' + this.userId,
      data: {
        metric: this.vm.metric,
        dimension: this.vm.dimension
      }
    }).then(function(data) {
      console.log(data);
      this.data = data;
      this.vm.loading = false;
    }.bind(this));
  }.bind(this));

  requestData();

  this.setMetric = function(key) {
    this.vm.metric = key;
    requestData();
  }.bind(this);

  this.setDimension = function(key) {
    this.vm.dimension = key;
    requestData();
  }.bind(this);

  this.trans = lichess.trans(env.i18n);
};
