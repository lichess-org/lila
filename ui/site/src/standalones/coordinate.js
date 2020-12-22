$(function () {
  $("#trainer").each(function () {
    var $trainer = $(this);
    var $board = $(".coord-trainer__board .cg-wrap");
    var ground;
    var $side = $(".coord-trainer__side");
    var $right = $(".coord-trainer__table");
    var $bar = $trainer.find(".progress_bar");
    var $coords = [$("#next_coord0"), $("#next_coord1"), $("#next_coord2")];
    var $start = $right.find(".start");
    var $explanation = $right.find(".explanation");
    var $score = $(".coord-trainer__score");
    var scoreUrl = $trainer.data("score-url");
    var duration = 30 * 1000;
    var tickDelay = 50;
    var colorPref = $trainer.data("color-pref");
    var color;
    var startAt, score;
    const $notation = $(".notation-0")[0] ? 0 : 1;

    var showColor = function () {
      color =
        colorPref == "random"
          ? ["white", "black"][Math.round(Math.random())]
          : colorPref;
      if (!ground)
        ground = Shogiground($board[0], {
          coordinates: false,
          drawable: { enabled: false },
          movable: {
            free: false,
            color: null,
          },
          orientation: color,
          addPieceZIndex: $("#main-wrap").hasClass("is3d"),
        });
      else if (color !== ground.state.orientation) ground.toggleOrientation();
      $trainer.removeClass("white black").addClass(color);
    };
    showColor();

    $trainer.find("form.color").each(function () {
      var $form = $(this);
      $form.find("input").on("change", function () {
        var selected = $form.find("input:checked").val();
        var c = {
          1: "white",
          2: "random",
          3: "black",
        }[selected];
        if (c !== colorPref) $.ajax(window.lishogi.formAjax($form));
        colorPref = c;
        showColor();
        return false;
      });
    });

    var showCharts = function () {
      var dark = $("body").hasClass("dark");
      var theme = {
        type: "line",
        width: "100%",
        height: "80px",
        lineColor: dark ? "#4444ff" : "#0000ff",
        fillColor: dark ? "#222255" : "#ccccff",
      };
      $side.find(".user_chart").each(function () {
        $(this).sparkline($(this).data("points"), theme);
      });
    };
    showCharts();

    var centerRight = function () {
      $right.css("top", 256 - $right.height() / 2 + "px");
    };
    centerRight();

    var clearCoords = function () {
      $.each($coords, function (i, e) {
        e.text("");
      });
    };

    var newCoord = function (prevCoord) {
      // disallow the previous coordinate's row or file from being selected
      var files = "123456789";
      var fileIndex = files.indexOf(prevCoord[0]);
      files = files.slice(0, fileIndex) + files.slice(fileIndex + 1, 9);

      var rows = "123456789";
      var rowIndex = rows.indexOf(prevCoord[1]);
      rows = rows.slice(0, rowIndex) + rows.slice(rowIndex + 1, 9);

      return codeCoords(
        files[Math.round(Math.random() * (files.length - 1))] +
          rows[Math.round(Math.random() * (rows.length - 1))]
      );
    };

    var advanceCoords = function () {
      $("#next_coord0").removeClass("nope");
      var lastElement = $coords.shift();
      $.each($coords, function (i, e) {
        e.attr("id", "next_coord" + i);
      });
      lastElement.attr("id", "next_coord" + $coords.length);
      lastElement.text(newCoord($coords[$coords.length - 1].text()));
      $coords.push(lastElement);
    };

    var stop = function () {
      clearCoords();
      $trainer.removeClass("play");
      centerRight();
      $trainer.removeClass("wrong");
      ground.set({
        events: {
          select: false,
        },
      });
      if (scoreUrl)
        $.ajax({
          url: scoreUrl,
          method: "post",
          data: {
            color: color,
            score: score,
          },
          success: function (charts) {
            $side.find(".scores").html(charts);
            showCharts();
          },
        });
    };

    var tick = function () {
      var spent = Math.min(duration, new Date() - startAt);
      $bar.css("width", (100 * spent) / duration + "%");
      if (spent < duration) setTimeout(tick, tickDelay);
      else stop();
    };

    function getShogiCoords(key) {
      const fileMap = {
        a: "9",
        b: "8",
        c: "7",
        d: "6",
        e: "5",
        f: "4",
        g: "3",
        h: "2",
        i: "1",
      };
      const rankMap = {
        9: "1",
        8: "2",
        7: "3",
        6: "4",
        5: "5",
        4: "6",
        3: "7",
        2: "8",
        1: "9",
      };
      return fileMap[key[0]] + rankMap[key[1]];
    }

    function codeCoords(key) {
      const rankMap1 = {
        1: "一",
        2: "二",
        3: "三",
        4: "四",
        5: "五",
        6: "六",
        7: "七",
        8: "八",
        9: "九",
      };
      switch ($notation) {
        // 11
        case 0:
          return key;
        // 1一
        default:
          return key[0] + rankMap1[key[1]];
      }
    }

    $start.click(function () {
      $explanation.remove();
      $trainer.addClass("play").removeClass("init");
      showColor();
      clearCoords();
      centerRight();
      score = 0;
      $score.text(score);
      $bar.css("width", 0);
      setTimeout(function () {
        startAt = new Date();
        ground.set({
          events: {
            select: function (key) {
              var hit = codeCoords(getShogiCoords(key)) == $coords[0].text();
              if (hit) {
                score++;
                $score.text(score);
                advanceCoords();
              } else {
                $("#next_coord0").addClass("nope");
                setTimeout(function () {
                  $("#next_coord0").removeClass("nope");
                }, 500);
              }
              $trainer.toggleClass("wrong", !hit);
            },
          },
        });
        $coords[0].text(newCoord("a1"));
        var i;
        for (i = 1; i < $coords.length; i++)
          $coords[i].text(newCoord($coords[i - 1].text()));
        tick();
      }, 1000);
    });
  });
});
