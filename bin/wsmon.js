var WebSocketClient = require('websocket').client;

var lichessSri = Math.random().toString(36).substring(2);

var url = 'ws://socket.en.lichess.org/lobby/socket';
var client = new WebSocketClient();
var averageLag;

var connect = function() {
  console.log('[info] Connect to ' + url);
  client.connect(url + '?sri=' + lichessSri);
};

client.on('connectFailed', function(error) {
  console.log('[error] connectFailed: ' + error.toString());
  setTimeout(connect, 5000);
});

client.on('connect', function(connection) {
  console.log('[info] Client connected. Average lag:');
  connection.on('error', function(error) {
    console.log('[error] ' + error.toString());
  });
  connection.on('close', function() {
    console.log('[info] Connection closed');
    averageLag = null;
    setTimeout(connect, 5000);
  });
  connection.on('message', function(message) {
    var data = JSON.parse(message.utf8Data);
    if (data.t == 'n') {
      var lag = new Date() - pingAt;
      if (lag > 100) console.log('[alert] High lag: ' + lag);
      if (!averageLag) averageLag = lag;
      else averageLag = 0.2 * (lag - averageLag) + averageLag;
      setTimeout(ping, 1000);
    }
  });

  var pingAt;
  var ping = function() {
    pingAt = new Date();
    connection.sendUTF(JSON.stringify({
      t: 'p',
      v: 0
    }));
  };
  ping();
});

connect();

setInterval(function() {
  if (averageLag !== null)
    console.log(Math.round(averageLag * 10) / 10);
}, 10000);
