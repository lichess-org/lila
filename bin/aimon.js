var HttpClient = require('request');

var nginx = (function () {/*
  server 216.218.250.236:9000 weight=8; # salim2
  server 91.209.78.225:9000 weight=12; # hexal
  server 91.121.16.158:9009 weight=4;
  # server 23.251.154.68:9009 weight=8; # nafis (expired)
  server 91.121.7.111:9009 weight=2; # paul
  # server 198.52.200.14:9009 weight=8; # drazak (expired)
  server jaldus.ddns.net:9009 weight=8; # jaldus
  server 91.121.223.148:9009 weight=4; # neary (ks3)
  server 91.121.150.217:9009 weight=5; # neary-tmp (ks6)
  server yubaba.mindleaking.org:10080 weight=3; # sarken
  server karmeliet.darklord.fr:9452 weight=3; # pat
*/}).toString().match(/[^]*\/\*([^]*)\*\/\}$/)[1];

var servers = nginx.split(/\n/).map(function(line) {
  return line.trim();
}).filter(function(line) {
  return line.length && line[0] != '#';
}).map(function(line) {
  var server = line.split(' ')[1];
  console.log(server);
  return server;
});

function checks() {
  servers.forEach(function(server) {
    var url = 'http://' + server + '/ai/move';
    HttpClient({
      url: url,
      timeout: 8000
    }, function (error, response, body) {
      if (error)
        console.log('[ERROR] ' + error + ' ' + server);
      else if (body.length != 4)
        console.log('[ERROR] ' + body + ' ' + server);
      else
        process.stdout.write('.');
        // console.log('[OK] ' + body + ' ' + server);
    });
  });
}

setInterval(checks, 10 * 60 * 1000);
checks();
