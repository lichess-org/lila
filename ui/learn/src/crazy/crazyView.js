// todo

var eventNames = ["touchstart", "mousedown"];

function sparePieces(position) {
  return m(
    "div",
    {
      class: ["spare", position, "orientation-" + "white", "white"].join(" "),
    },
    ["lance", "rook", "bishop", "knight", "pawn"].map(function (role) {
      return m(
        "div.no-square",
        m("piece", {
          class: "white" + " " + role,
          "data-color": "white",
          "data-role": role,
        })
      );
    })
  );
}

m(
  "div",
  {
    config: function (el, isUpdate, context) {
      if (isUpdate) return;
      var onstart = partial(drag, ground.instance);
      eventNames.forEach(function (name) {
        document.addEventListener(name, onstart);
      });
      context.onunload = function () {
        eventNames.forEach(function (name) {
          document.removeEventListener(name, onstart);
        });
      };
    },
  },
  [sparePieces("bottom")]
);
