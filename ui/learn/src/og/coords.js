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

module.exports = function (orientation, el) {
  util.requestAnimationFrame(function () {
    var coords = document.createDocumentFragment();
    var orientClass = orientation === "black" ? " black" : "";
    coords.appendChild(
      renderCoords(
        ["9", "8", "7", "6", "5", "4", "3", "2", "1"],
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
    for (i = 0; i < coords.length; ++i)
      coords[i].classList.toggle("black", o === "black");
  };
};
