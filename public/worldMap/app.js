var stats = {
  nMoves: 0,
  countries: {}
};

var context, gain_node, filter, oscillator, sound = {};
var noteIt = 0;

sound.stop = function() {
  try {
    oscillator.disconnect();
  } catch (e) {}
};

sound.play = function(freq) {
  oscillator = context.createOscillator();
  oscillator.type = 'sawtooth';
  oscillator.connect(filter);
  oscillator.frequency.value = freq;
  oscillator.start(0);
};

(function init(g) {
  try {
    context = new(g.AudioContext || g.webkitAudioContext);
    gain_node = context.createGain();
    gain_node.connect(context.destination);
    gain_node.gain.value = 0.1;
    filter = context.createBiquadFilter();
    filter.type = 'lowpass';
    filter.connect(gain_node);
  } catch (e) {
    console.log('No web audio oscillator support in this browser');
  }
}(window));

var soundEnabled = false;
var prevMoves;
var maxPrevMoves = 10;
setInterval(function() {
  if (!soundEnabled) return;
  sound.stop();
  if (stats.nMoves > 0) {
    noteIt++;
    if (prevMoves) {
      var note = Math.min(
        11 + 3 * (stats.nMoves - prevMoves),
        79);
      var freq = MIDIUtils.noteNumberToFrequency(note);
      sound.play(freq);
    }
    filter.frequency.value = 2400 + Math.sin(noteIt / 22) * 2000;
    filter.Q.value = 6 + Math.sin(noteIt / 9) * 12;
    gain_node.gain.value = 0.3 + -0.08 * Math.sin(noteIt / 22);
    prevMoves = stats.nMoves;
  }
}, 100);

$('body').prepend($('<button id="sound">').text('SOUND').click(function() {
  soundEnabled = !soundEnabled;
  prevMoves = null;
  if (!soundEnabled) sound.stop();
}));

$(function() {
  var worldWith = window.innerWidth - 30,
    mapRatio = 0.4,
    worldHeight = worldWith * mapRatio,
    scale = worldWith / 1000;
  var paper = Raphael(document.getElementById("worldmap"), worldWith, worldHeight);
  paper.rect(0, 0, worldWith, worldHeight, 10).attr({
    stroke: "none"
  });
  paper.setStart();
  for (var country in worldmap.shapes) {
    paper.path(worldmap.shapes[country]).attr({
      'stroke': '#05121b',
      'stroke-width': 0.5,
      'stroke-opacity': 0.25,
      fill: "#67777F",
      "fill-opacity": 0.25
    }).transform("s" + scale + "," + scale + " 0,0");
  }
  var world = paper.setFinish();
  world.getXY = function(lat, lon) {
    return {
      cx: lon * (2.6938 * scale) + (465.4 * scale),
      cy: lat * (-2.6938 * scale) + (227.066 * scale)
    };
  };

  // position legend and stats
  var worldOffset = $('#worldmap').offset();
  $('#stats').offset({
    top: worldOffset.top + 250 * scale,
    left: worldOffset.left + 7.6 * scale
  }).attr({
    width: 195 * scale,
    height: 172 * scale
  });

  if (!!window.EventSource) {
    var source = new EventSource("/network/stream");

    source.addEventListener('message', function(e) {
      var raw = e.data.split('|');
      var data = {
        country: raw[0],
        lat: parseFloat(raw[1]),
        lon: parseFloat(raw[2]),
        oLat: parseFloat(raw[3]),
        oLon: parseFloat(raw[4])
        // move: raw[5],
        // piece: raw[6]
      };
      data.lat += Math.random() - 0.5;
      data.lon += Math.random() - 0.5;
      var orig = world.getXY(data.lat, data.lon);
      var dot = paper.circle().attr({
        fill: "#FE7727",
        r: 1.2,
        'stroke-width': 0
      });
      dot.attr(orig);
      setTimeout(function() {
        dot.remove();
      }, 5000);
      if (data.oLat) {
        var dest = world.getXY(data.oLat, data.oLon);
        dest.lat += Math.random() - 0.5;
        dest.lon += Math.random() - 0.5;
        var str = "M" + orig.cx + "," + orig.cy + "T" + dest.cx + "," + dest.cy;
        var lightning = paper.path(str);
        lightning.attr({
          opacity: 0.2,
          stroke: "#fff",
          'stroke-width': 0.5
        });
        setTimeout(function() {
          lightning.remove();
          var line = paper.path(str);
          line.attr({
            opacity: 0.35,
            stroke: "#FE7727",
            'stroke-width': 0.5,
            'arrow-end': 'oval-wide-long'
          });
          setTimeout(function() {
            line.remove();
            var drag = paper.path(str);
            drag.attr({
              opacity: 0.2,
              stroke: "#FE7727",
              'stroke-width': 0.5,
              'arrow-end': 'oval-wide-long'
            });
            setTimeout(function() {
              drag.remove();
            }, 500);
          }, 500);
        }, 50);
      }

      // moves
      stats.nMoves++;
      $('#moves > span').text(stats.nMoves);
      // top countries
      if (data.country in stats.countries) stats.countries[data.country]++;
      else stats.countries[data.country] = 1;
    }, false);
    // source.addEventListener('open', function(e) {
    //   // Connection was opened.
    //   // console.log("connection opened");
    // }, false);
    // source.addEventListener('error', function(e) {
    //   if (e.readyState == EventSource.CLOSED) {
    //     // Connection was closed.
    //     // console.log("connection closed");
    //   }
    // }, false);
  }
});
