var m = require('mithril');
var partial = require('chessground').util.partial;
var pgnExport = require('./pgnExport');

module.exports = {
  ctrl: function(cfg) {

    var forecasts = [
      'd5 c3 Nf6 a3',
      'b6 Qh5 Nc6 Nc3 g6 a3',
      'a6 a3'
    ];

    return {
      addSans: function(sans) {
        // must end with player move
        if (sans.size % 2 !== 0) sans.pop();
        if (sans.size === 0) return;
        forecasts.push(fc.join(' '));
        forecasts = lichess.unique(forecasts);
      },
      isCandidate: function(sans) {},
      removeIndex: function(index) {
        forecasts = forecasts.filter(function(fc, i) {
          return i !== index;
        });
      },
      listSans: function() {
        return forecasts.map(function(f) {
          return f.split(' ');
        });
      },
      gamePly: function() {
        return cfg.gamePly;
      }
    };
  },
  view: function(ctrl) {
    var fctrl = ctrl.forecast;
    var current = pgnExport.arraySince(ctrl, fctrl.gamePly());
    var isCandidate = current.length > 1;
    return m('div.forecast', [
      m('div.box', [
        m('div.top', 'Conditional premoves'),
        m('div.list', fctrl.listSans().map(function(fc, i) {
          return m('div.entry', {
            'data-icon': 'G',
            class: 'text'
          }, [
            m('a', {
              class: 'delete',
              onclick: partial(fctrl.removeIndex, i)
            }, 'x'),
            m.trust(pgnExport.renderSanSince(fc, fctrl.gamePly()))
          ])
        })),
        m('button', {
          class: 'add button text',
          'data-icon': isCandidate ? 'O' : "",
          'disabled': !isCandidate,
        }, isCandidate ? [
          m('span', 'Add current variation'),
          m('span', current.join(' '))
        ] : [
          m('span', 'Play a variation to create'),
          m('span', 'conditional premoves')
        ])
      ])
    ]);
  }
};
