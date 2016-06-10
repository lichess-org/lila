$(function() {
  var loading = true,
    worldWith = window.innerWidth - 30,
    mapRatio = 0.4,
    worldHeight = worldWith * mapRatio,
    scale = worldWith / 1000;
  var paper = Raphael(document.getElementById("worldmap"), worldWith, worldHeight);
  paper.rect(0, 0, worldWith, worldHeight, 10).attr({
    stroke: "none"
  });
  paper.setStart();
  worldShapes.forEach(function(shape) {
    paper.path(shape).attr({
      'stroke': '#05121b',
      'stroke-width': 0.5,
      'stroke-opacity': 0.25,
      fill: "#1d2b33"
    }).transform("s" + scale + "," + scale + " 0,0");
  });
  var world = paper.setFinish();
  world.getXY = function(lat, lon) {
    return {
      cx: lon * (2.6938 * scale) + (465.4 * scale),
      cy: lat * (-2.6938 * scale) + (227.066 * scale)
    };
  };

  var randomPoint = function() {
    return [
      Math.random() * 100 - 50,
      Math.random() * 200 - 100
    ];
  };

  var point2pos = function(point) {
    // point = randomPoint();
    return world.getXY(
      point[0] + Math.random() - 0.5,
      point[1] + Math.random() - 0.5);
  };

  var source = new EventSource("//en.lichess.org/network/stream");

  var removeFunctions = {};

  var appearPoint = function(pos) {
    var appear = paper.circle().attr({
      opacity: 0.4,
      fill: "#fff",
      r: 3,
      'stroke-width': 0
    }).attr(pos);
    setTimeout(function() {
      appear.remove();
    }, 130);
  }

  var drawPoint = function(pos) {
    var dot = paper.circle().attr({
      opacity: 0.5,
      fill: "#fff",
      stroke: "#FE7727",
      r: 1.5,
      'stroke-width': 1
    }).attr(pos);
    if (!loading) appearPoint(pos);
    return function() {
      dot.remove();
      appearPoint(pos);
    };
  };

  var appearLine = function(pos) {
    var appear = paper.path(
      "M" + pos[0].cx + "," + pos[0].cy + "T" + pos[1].cx + "," + pos[1].cy
    ).attr({
      opacity: 0.25,
      stroke: "#fff",
      'stroke-width': 1
    });
    setTimeout(function() {
      appear.remove();
    }, 130);
  }

  var drawLine = function(pos) {
    var line = paper.path(
      "M" + pos[0].cx + "," + pos[0].cy + "T" + pos[1].cx + "," + pos[1].cy
    ).attr({
      opacity: 0.12,
      stroke: "#FE7727",
      'stroke-width': 1
    });
    if (!loading) appearLine(pos);
    return function() {
      line.remove();
      appearLine(pos);
    };
  }

  source.addEventListener('message', function(e) {
    var data = JSON.parse(e.data);
    if (data.loadComplete) loading = false;
    if (removeFunctions[data.id]) {
      removeFunctions[data.id].forEach(function(f) {
        f();
      });
      delete removeFunctions[data.id];
    }
    if (!data.ps) return;
    var pos = data.ps.map(point2pos);
    removeFunctions[data.id] = pos.map(drawPoint);
    if (data.ps[1]) removeFunctions[data.id].push(drawLine(pos));
  }, false);

  setTimeout(function() {
    loading = false;
  }, 10000);
});
