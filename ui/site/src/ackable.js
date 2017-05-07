module.exports = function(send) {

  var messages = [];

  function resend() {
    var resendCutoff = Date.now() - 2500;
    messages.forEach(function(m) {
      if (m.at < resendCutoff) send(m.t, m.d);
    });
  }

  setInterval(resend, 1000);

  return {
    resend: resend,
    register: function(t, d) {
      messages.push({
        t: t,
        d: d,
        at: Date.now()
      });
    },
    gotAck: function() {
      messages = [];
    }
  };
}
