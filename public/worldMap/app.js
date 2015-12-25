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

  var source = new EventSource("/network/stream");

  var removeFunctions = {};

  var drawPoint = function(pos) {
    var dot = paper.circle().attr({
      opacity: 0.5,
      fill: "#fff",
      stroke: "#FE7727",
      r: 1.5,
      'stroke-width': 1
    }).attr(pos);
    // var shadow = paper.circle().attr({
    //   opacity: 0.008,
    //   fill: "#fff",
    //   r: 10 + Math.random() * 30
    // }).attr(pos);
    return function() {
      dot.remove();
      // shadow.remove();
    };
  };

  var drawLine = function(pos) {
    var line = paper.path(
      "M" + pos[0].cx + "," + pos[0].cy + "T" + pos[1].cx + "," + pos[1].cy
    ).attr({
      opacity: 0.12,
      stroke: "#FE7727",
      'stroke-width': 1
    });
    return function() {
      line.remove();
    };
  }

  source.addEventListener('message', function(e) {
    var data = JSON.parse(e.data);
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
});
