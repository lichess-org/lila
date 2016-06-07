var WebSocketClient = require('websocket').client;
var HttpClient = require('request');

var lichessSri = Math.random().toString(36).substring(2);

var url = 'wss://socket.lichess.org/socket';
var client = new WebSocketClient();
var geoLag = process.argv[2];
var password = process.argv[3];
if (!password) throw "missing SMS password";
var averageLag;
var lagAlert = 50; // in milliseconds
var fail = 0;
var failAlert = 2000;

var logger = function(sev, msg) {
  console.log();
  console.log(new Date() + ' [' + sev + '] ' + msg);
};

logger('info', 'Geographical lag: ' + geoLag);

var connect = function() {
  logger('info', 'Connect to ' + url);
  client.connect(url + '?sri=' + lichessSri);
};

var sendSms = function(msg) {
  logger('error', 'Sending SMS now');
  HttpClient.post({
      url: 'https://smsapi.free-mobile.fr/sendmsg',
      method: 'post',
      rejectUnauthorized: false,
      qs: {
        user: '21942578',
        pass: password,
        msg: '[wsmon] lichess.org: ' + msg
      }
    },
    function(error, response, body) {
      if (error) logger('error', 'SMS sent: ' + error);
      else logger('info', 'SMS sent: ' + response.statusCode + ' ' + body);
    });
};

var addFail = function(v) {
  fail += v;
  if (fail >= failAlert) {
    sendSms('fail = ' + fail);
    fail = 0;
  }
};

client.on('connectFailed', function(error) {
  logger('error', 'connectFailed: ' + error.toString());
  addFail(200);
  setTimeout(connect, 5000);
});

client.on('connect', function(connection) {
  logger('info', 'Client connected. Average lag:');
  connection.on('error', function(error) {
    addFail(200);
    logger('error', error.toString());
  });
  connection.on('close', function() {
    addFail(200);
    logger('info', 'Connection closed');
    averageLag = null;
    setTimeout(connect, 5000);
  });
  connection.on('message', function(message) {
    var data = JSON.parse(message.utf8Data);
    if (data.t == 'n') {
      var lag = Math.max(0, new Date() - pingAt - geoLag);
      if (lag > lagAlert) {
        addFail(Math.round(lag / 10));
        logger('warn', 'High lag: ' + lag);
      } else {
        fail = Math.max(0, fail - 1);
      }
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
  if (fail > 0)
    process.stdout.write('{' + fail + '} ');
}, 10000);
