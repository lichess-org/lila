var m = require("mithril");
var util = require("./util");

function renderCoords(elems, klass, orient) {
  var el = document.createElement("coords");
  el.className = klass;
  elems.forEach(function (content) {
    var f = document.createElement("coord");
    f.textContent = content;
    el.appendChild(f);
  });
  return el;
}

module.exports = function (data, el) {
  util.requestAnimationFrame(function () {
    var coords = document.createDocumentFragment();
    var orientClass = data.orientation === "black" ? " black" : "";
    const westernNotation = ["9", "8", "7", "6", "5", "4", "3", "2", "1"];
    const japaneseNotation = [
      "九",
      "八",
      "七",
      "六",
      "五",
      "四",
      "三",
      "二",
      "一",
    ];
    coords.appendChild(
      renderCoords(
        data.notation === 0 ? westernNotation : japaneseNotation,
        "ranks" + orientClass
      )
    );
    coords.appendChild(
      renderCoords(
        ["9", "8", "7", "6", "5", "4", "3", "2", "1"],
        "files" + orientClass
      )
    );
    el.appendChild(coords);
  });

  var orientation;

  return function (o) {
    if (o === orientation) return;
    orientation = o;
    var coords = el.querySelectorAll("coords");
    for (var i = 0; i < coords.length; ++i)
      coords[i].classList.toggle("black", o === "black");
  };
};
