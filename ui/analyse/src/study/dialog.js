var m = require('mithril');

module.exports = {
  form: function(cfg) {
    return m('div.lichess_overboard.study_overboard', {
      class: cfg.class,
      config: function(el, isUpdate) {
        if (!isUpdate) lichess.loadCss('/assets/stylesheets/material.form.css');
      }
    }, [
      m('a.close.icon[data-icon=L]', {
        onclick: cfg.onClose
      }),
      cfg.content,
      cfg.button ? m('div.button-container',
        m('button.submit.button[type=submit]', cfg.button)
      ) : null
    ]);
  }
};
