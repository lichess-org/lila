var WebSocketClient = require('websocket').client;

var lichessSri = Math.random().toString(36).substring(2);

var url = 'ws://socket.en.lichess.org/lobby/socket';
var client = new WebSocketClient();

var connect = function() {
  console.log('Connect to ' + url);
  client.connect(url + '?sri=' + lichessSri);
};

client.on('connectFailed', function(error) {
  console.log('Connect Error: ' + error.toString());
  setTimeout(connect, 5000);
});

client.on('connect', function(connection) {
  console.log('WebSocket client connected');
  connection.on('error', function(error) {
    console.log("Connection Error: " + error.toString());
  });
  connection.on('close', function() {
    console.log('echo-protocol Connection Closed');
    setTimeout(connect, 5000);
  });
  connection.on('message', function(message) {
    var data = JSON.parse(message.utf8Data);
    if (data.t == 'n') {
      var lag = new Date() - pingAt;
      process.stdout.write(lag + ' ');
      if (lag > 100) console.log('Lag alert: ' + lag);
      setTimeout(ping, 5000);
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
