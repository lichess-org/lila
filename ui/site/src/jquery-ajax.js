$.ajaxSetup({
  cache: false
});
$.ajaxTransport('script', function(s) {
  // Monkeypatch jQuery to load scripts with nonce. Upstream patch:
  // - https://github.com/jquery/jquery/pull/3766
  // - https://github.com/jquery/jquery/pull/3782
  // Original transport:
  // https://github.com/jquery/jquery/blob/master/src/ajax/script.js
  var script, callback;
  return {
    send: function(_, complete) {
      script = $("<script>").prop({
        nonce: document.body.getAttribute('data-nonce'), // Add the nonce!
        charset: s.scriptCharset,
        src: s.url
      }).on("load error", callback = function(evt) {
        script.remove();
        callback = null;
        if (evt) {
          complete(evt.type === "error" ? 404 : 200, evt.type);
        }
      });
      document.head.appendChild(script[0]);
    },
    abort: function() {
      if (callback) {
        callback();
      }
    }
  };
});
