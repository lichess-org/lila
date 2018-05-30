module.exports = function(send) {

  var currentId = 1; // increment with each ackable message sent

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
      d.a = currentId++;
      messages.push({
        t: t,
        d: d,
        at: Date.now()
      });
    },
    gotAck: function(id) {
      messages = messages.filter(function(m) {
        return m.d.a !== id;
      });
    }
  };
}
