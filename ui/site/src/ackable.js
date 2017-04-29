module.exports = function(send) {

  var messages = [];

  function resend() {
    messages.forEach(function(m) {
      if (Date.now() - m.at > 2500) send(m.t, m.d);
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
