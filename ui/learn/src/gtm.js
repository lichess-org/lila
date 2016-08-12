var stages = require('./stage/list');

module.exports = {
  onComplete: function(data, stage) {
    var categ = stages.categs[stages.stageIdToCategId(stage.id)];
    var complete = categ.stages.every(function(s) {
      return !!data.stages[s.key];
    });
    if (complete) window.dataLayer.push({
      'event': 'VirtualPageview',
      'virtualPageURL': '/' + categ.key,
      'virtualPageTitle': categ.name
    });
  }
};
