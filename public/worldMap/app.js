var stats = {
  nMoves: 0,
  countries: {}
};

var audio_context, oscillator, sound = {};

sound.stop = function() {
  try {
    oscillator.noteOff(0);
  } catch (e) {}
};

sound.play = function(freq) {
  oscillator = audio_context.createOscillator();
  oscillator.type = 'square';
  oscillator.connect(audio_context.destination);
  oscillator.frequency.value = freq;
  oscillator.noteOn(0);
};

(function init(g) {
  try {
    audio_context = new(g.AudioContext || g.webkitAudioContext);
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
    if (prevMoves) {
      var note = 30 + (stats.nMoves - prevMoves);
      var freq = MIDIUtils.noteNumberToFrequency(note);
      sound.play(freq);
    }
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
      stroke: null,
      fill: "#67777F",
      "fill-opacity": 0.3
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
    var source = new EventSource("http://en.lichess.org/world-map/stream");

    source.addEventListener('message', function(e) {
      var data = JSON.parse(e.data);
      data.lat += Math.random() * 2 - 1;
      data.lon += Math.random() * 2 - 1;
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
      var area = paper.circle().attr({
        fill: "#fff",
        'fill-opacity': Math.random() * 0.015,
        r: 20 + Math.random() * 50,
        'stroke-width': 0
      });
      area.attr(orig);
      setTimeout(function() {
        area.remove();
      }, 4000);
      if (data.oLat) {
        var dest = world.getXY(data.oLat, data.oLon);
        dest.lat += Math.random() - 0.5;
        dest.lon += Math.random() - 0.5;
        var str = "M" + orig.cx + "," + orig.cy + "T" + dest.cx + "," + dest.cy;
        var lightning = paper.path(str);
        lightning.attr({
          opacity: 0.2,
          stroke: "#fff"
        });
        setTimeout(function() {
          lightning.remove();
          var line = paper.path(str);
          line.attr({
            opacity: 0.35,
            stroke: "#FE7727",
            'arrow-end': 'oval-wide-long'
          });
          setTimeout(function() {
            line.remove();
          }, 700);
        }, 50);
      }

      // moves
      stats.nMoves++;
      $('#moves > span').text(stats.nMoves);
      // top countries
      if (data.country in stats.countries) stats.countries[data.country]++;
      else stats.countries[data.country] = 1;
    }, false);
    source.addEventListener('open', function(e) {
      // Connection was opened.
      // console.log("connection opened");
    }, false);
    source.addEventListener('error', function(e) {
      if (e.readyState == EventSource.CLOSED) {
        // Connection was closed.
        // console.log("connection closed");
      }
    }, false);
  }
});
