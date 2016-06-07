var WebSocketClient = require('websocket').client;
var HttpClient = require('request');

var lichessSri = Math.random().toString(36).substring(2);

var client = new WebSocketClient();
var geoLag = process.argv[2];
var password = process.argv[3];
if (!password) throw "missing SMS password";
var domain = process.argv[4] || 'lichess.org';
var url = 'wss://socket.' + domain + '/socket';
var averageLag;
var lagAlert = 100; // in milliseconds
var fail = 0;
var failAlert = 10 * 1000;

var logger = function(sev, msg) {
  console.log();
  console.log(new Date() + ' [' + sev + '] ' + msg);
};

var runCommand = function(cmd) {
  logger('error', 'Running ' + cmd);
  var exec = require('child_process').exec;
  exec(cmd, function(error, stdout, stderr) {
    console.log(error);
    console.log(stdout);
    console.log(stderr);
  });
};
// runCommand('jinfo `cat /home/lichess/RUNNING_PID`');

var lastRestarted = 0;

var doRestartLichess = function() {
  lastRestarted = new Date().getTime();
  logger('error', 'Restart lichess now');
  runCommand('/home/lichess/bin/prod/restart-now');
}

var restartLichess = function(msg) {
  logger('error', msg);
  logger('error', "Asking to restart");
  if (new Date().getTime() < lastRestarted + 5 * 60 * 1000) logger('error', "Too early!");
  else doRestartLichess();
  // runCommand('jstack -F `cat /home/lichess/RUNNING_PID` > /root/lichess-auto-jstack');
  // setTimeout(function() {
  // doRestartLichess();
  // }, 3000);
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
        msg: '[wsmon] ' + domain + ': ' + msg
      }
    },
    function(error, response, body) {
      if (error) logger('error', 'SMS sent: ' + error);
      else logger('info', 'SMS sent: ' + response.statusCode + ' ' + body);
    });
};

// sendSms("test");

var addFail = function(v) {
  fail += v;
  if (fail >= failAlert) {
    fail = 0;
    restartLichess('fail = ' + fail);
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
      } else if (lag >= 10) {
        process.stdout.write("(" + lag + ") ");
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
  //  if (fail > 0)
  //    process.stdout.write('{' + fail + '} ');
}, 10000);

// HTTP
(function() {
  var initialErrorRetryDelay = 3 * 60 * 1000;
  var errorRetryDelay = initialErrorRetryDelay;
  var testDelay = 5000;
  var errorCount = 0;
  var errorInc = 5;
  var errorThreshold = 2 * 60 * 1000 / testDelay * errorInc;
  console.log(errorInc, 'errorInc');
  console.log(errorThreshold, 'errorThreshold');

  function onError(err) {
    logger('error', 'httpFailed: ' + err);
    errorCount += errorInc;
    logger('error', 'HTTP error count: ' + errorCount + '/' + errorThreshold);
    if (errorCount < errorThreshold) {
      setTimeout(httpCheck, testDelay);
      return;
    }
    restartLichess('HTTP(' + errorCount + ') = ' + err);
    errorCount = 0;
    logger('error', 'will retry in ' + (errorRetryDelay / 1000 / 60) + ' minutes');
    setTimeout(httpCheck, errorRetryDelay);
    errorRetryDelay = errorRetryDelay * 2;
  }

  function httpCheck() {
    HttpClient.get('http://en.' + domain, {
      timeout: 1000
    }).on('response', function(res) {
      if (res.statusCode != 200) return onError(res.statusCode);
      if (errorCount > 0) errorCount--;
      errorRetryDelay = initialErrorRetryDelay;
      setTimeout(httpCheck, testDelay);
    }).on('error', onError);
  }
  setTimeout(httpCheck, testDelay);
})();
