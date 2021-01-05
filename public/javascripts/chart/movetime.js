function toBlurArray(player) {
  return player.blurs && player.blurs.bits ? player.blurs.bits.split("") : [];
}
lishogi.movetimeChart = function (data, trans, notation) {
  if (!data.game.moveCentis) return; // imported games
  lishogi.loadScript("javascripts/chart/common.js").done(function () {
    lishogi.loadScript("javascripts/chart/division.js").done(function () {
      lishogi.chartCommon("highchart").done(function () {
        lishogi.movetimeChart.render = function () {
          $("#movetimes-chart:not(.rendered)").each(function () {
            var $this = $(this).addClass("rendered");

            var series = {
              white: [],
              black: [],
            };

            var tree = data.treeParts;
            var ply = 0;
            var max = 0;

            var logC = Math.pow(Math.log(3), 2);

            var blurs = [toBlurArray(data.player), toBlurArray(data.opponent)];
            if (data.player.color === "white") blurs.reverse();

            data.game.moveCentis.forEach(function (time, i) {
              var node = tree[i + 1];
              ply = node ? node.ply : ply + 1;
              var san = node
                ? notation({ san: node.san, uci: node.uci, fen: node.fen })
                : "-";

              var color = ply & 1;

              var y =
                Math.pow(Math.log(0.005 * Math.min(time, 12e4) + 3), 2) - logC;
              max = Math.max(y, max);

              var point = {
                name: ply + ". " + san,
                x: i,
                y: color ? y : -y,
              };

              if (blurs[color].shift() === "1") {
                point.marker = {
                  symbol: "square",
                  radius: 3,
                  lineWidth: "1px",
                  lineColor: "#3893E8",
                  fillColor: color ? "#fff" : "#333",
                };
                point.name += " [blur]";
              }

              series[color ? "white" : "black"].push(point);
            });

            var disabled = {
              enabled: false,
            };
            var noText = {
              text: null,
            };
            $this.highcharts({
              credits: disabled,
              legend: disabled,
              series: [
                {
                  name: "White",
                  data: series.white,
                },
                {
                  name: "Black",
                  data: series.black,
                },
              ],
              chart: {
                type: "area",
                spacing: [2, 0, 2, 0],
                animation: false,
              },
              tooltip: {
                formatter: function () {
                  var seconds = data.game.moveCentis[this.x] / 100;
                  if (seconds) seconds = seconds.toFixed(seconds >= 2 ? 1 : 2);
                  return (
                    this.point.name +
                    "<br />" +
                    trans("nbSeconds", "<strong>" + seconds + "</strong>")
                  );
                },
              },
              plotOptions: {
                series: {
                  animation: false,
                },
                area: {
                  fillColor: Highcharts.theme.lishogi.area.black,
                  negativeFillColor: Highcharts.theme.lishogi.area.white, // swapped
                  fillOpacity: 1,
                  threshold: 0,
                  lineWidth: 1,
                  color: "#3893E8",
                  allowPointSelect: true,
                  cursor: "pointer",
                  states: {
                    hover: {
                      lineWidth: 1,
                    },
                  },
                  events: {
                    click: function (event) {
                      if (event.point) {
                        event.point.select();
                        lishogi.pubsub.emit(
                          "analysis.chart.click",
                          event.point.x
                        );
                      }
                    },
                  },
                  marker: {
                    radius: 1,
                    states: {
                      hover: {
                        radius: 3,
                        lineColor: "#3893E8",
                        fillColor: "#ffffff",
                      },
                      select: {
                        radius: 4,
                        lineColor: "#3893E8",
                        fillColor: "#ffffff",
                      },
                    },
                  },
                },
              },
              title: noText,
              xAxis: {
                title: noText,
                labels: disabled,
                lineWidth: 0,
                tickWidth: 0,
                plotLines: lishogi.divisionLines(data.game.division, trans),
              },
              yAxis: {
                title: noText,
                min: -max,
                max: max,
                labels: disabled,
                gridLineWidth: 0,
              },
            });
          });
          lishogi.pubsub.emit("analysis.change.trigger");
        };
        lishogi.movetimeChart.render();
      });
    });
  });
};
