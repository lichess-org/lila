var WebSocketClient = require('websocket').client;

var lichessSri = Math.random().toString(36).substring(2);

var url = 'ws://socket.en.lichess.org/socket';
var client = new WebSocketClient();
var averageLag;

var logger = function(sev, msg) {
  console.log();
  console.log(new Date() + ' [' + sev + '] ' + msg);
};

var connect = function() {
  logger('info', 'Connect to ' + url);
  client.connect(url + '?sri=' + lichessSri);
};

client.on('connectFailed', function(error) {
  logger('error', 'connectFailed: ' + error.toString());
  setTimeout(connect, 5000);
});

client.on('connect', function(connection) {
  logger('info', 'Client connected. Average lag:');
  connection.on('error', function(error) {
    logger('error', error.toString());
  });
  connection.on('close', function() {
    logger('info', 'Connection closed');
    averageLag = null;
    setTimeout(connect, 5000);
  });
  connection.on('message', function(message) {
    var data = JSON.parse(message.utf8Data);
    if (data.t == 'n') {
      var lag = new Date() - pingAt;
      if (lag > 100) logger('warn', 'High lag: ' + lag);
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
    process.stdout.write(Math.round(averageLag) + ' ');
}, 10000);
