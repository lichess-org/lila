import AnalyseCtrl from "./ctrl";
import { defined } from "common";
import { baseUrl } from "./util";
import { AnalyseData } from "./interfaces";
import {westernShogiNotation, displaySfen} from "shogiutil/util";

export default function (element: HTMLElement, ctrl: AnalyseCtrl) {
  const li = window.lishogi;

  $(element).replaceWith(ctrl.opts.$underboard!);

  $("#adv-chart").attr("id", "acpl-chart");

  const data = ctrl.data,
    $panels = $(".analyse__underboard__panels > div"),
    $menu = $(".analyse__underboard__menu"),
    $timeChart = $("#movetimes-chart"),
    inputFen = document.querySelector(
      ".analyse__underboard__fen"
    ) as HTMLInputElement,
    unselect = (chart) => {
      chart.getSelectedPoints().forEach(function (point) {
        point.select(false);
      });
    };
  let lastFen: string;

  if (!li.AnalyseNVUI) {
    li.pubsub.on("analysis.comp.toggle", (v: boolean) => {
      setTimeout(function () {
        (v
          ? $menu.find('[data-panel="computer-analysis"]')
          : $menu.find("span:eq(1)")
        ).trigger("mousedown");
      }, 50);
    });
    li.pubsub.on("analysis.change", (fen: Fen, _, mainlinePly: Ply | false) => {
      let chart,
        point,
        $chart = $("#acpl-chart");
      if (fen && fen !== lastFen) {
        inputFen.value = displaySfen(fen);
        lastFen = fen;
      }
      if ($chart.length) {
        chart = window.Highcharts && $chart.highcharts();
        if (chart) {
          if (mainlinePly != chart.lastPly) {
            if (mainlinePly === false) unselect(chart);
            else {
              point =
                chart.series[0].data[mainlinePly - 1 - data.game.startedAtTurn];
              if (defined(point)) point.select();
              else unselect(chart);
            }
          }
          chart.lastPly = mainlinePly;
        }
      }
      if ($timeChart.length) {
        chart = window.Highcharts && $timeChart.highcharts();
        if (chart) {
          if (mainlinePly != chart.lastPly) {
            if (mainlinePly === false) unselect(chart);
            else {
              const white = mainlinePly % 2 !== 0;
              const serie = white ? 0 : 1;
              const turn = Math.floor(
                (mainlinePly - 1 - data.game.startedAtTurn) / 2
              );
              point = chart.series[serie].data[turn];
              if (defined(point)) point.select();
              else unselect(chart);
            }
          }
          chart.lastPly = mainlinePly;
        }
      }
    });
    li.pubsub.on("analysis.server.progress", (d: AnalyseData) => {
      if (!li.advantageChart) startAdvantageChart();
      else if (li.advantageChart.update) li.advantageChart.update(d);
      if (d.analysis && !d.analysis.partial) $("#acpl-chart-loader").remove();
    });
  }

  function chartLoader() {
    return `<div id="acpl-chart-loader"><span>Stockfish 11+<br>server analysis</span>${li.spinnerHtml}</div>`;
  }
  function startAdvantageChart() {
    if (li.advantageChart || li.AnalyseNVUI) return;
    const loading =
      !data.treeParts[0].eval || !Object.keys(data.treeParts[0].eval).length;
    const $panel = $panels.filter(".computer-analysis");
    if (!$("#acpl-chart").length)
      $panel.html(
        '<div id="acpl-chart"></div>' + (loading ? chartLoader() : "")
      );
    else if (loading && !$("#acpl-chart-loader").length)
      $panel.append(chartLoader());
    li.loadScript("javascripts/chart/acpl.js").then(function () {
      li.advantageChart(data, ctrl.trans, $("#acpl-chart")[0] as HTMLElement);
    });
  }

  const storage = li.storage.make("analysis.panel");
  const setPanel = function (panel) {
    $menu
      .children(".active")
      .removeClass("active")
      .end()
      .find(`[data-panel="${panel}"]`)
      .addClass("active");
    $panels
      .removeClass("active")
      .filter("." + panel)
      .addClass("active");
    if ((panel == "move-times" || ctrl.opts.hunter) && !li.movetimeChart)
      try {
        li.loadScript("javascripts/chart/movetime.js").then(function () {
          li.movetimeChart(data, ctrl.trans, westernShogiNotation);
        });
      } catch (e) {}
    if (
      (panel == "computer-analysis" || ctrl.opts.hunter) &&
      $("#acpl-chart").length
    )
      setTimeout(startAdvantageChart, 200);
  };
  $menu.on("mousedown", "span", function (this: HTMLElement) {
    const panel = $(this).data("panel");
    storage.set(panel);
    setPanel(panel);
  });
  const stored = storage.get();
  if (stored && $menu.children(`[data-panel="${stored}"]:visible`).length)
    setPanel(stored);
  else {
    const $menuCt = $menu.children('[data-panel="ctable"]');
    ($menuCt.length ? $menuCt : $menu.children(":first-child")).trigger(
      "mousedown"
    );
  }
  if (!data.analysis) {
    $panels
      .find("form.future-game-analysis")
      .submit(function (this: HTMLElement) {
        if ($(this).hasClass("must-login")) {
          if (confirm(ctrl.trans("youNeedAnAccountToDoThat")))
            location.href = "/signup";
          return false;
        }
        $.ajax({
          ...li.formAjax($(this)),
          success: startAdvantageChart,
          error: li.reload,
        });
        return false;
      });
  }

  $panels.on("click", ".pgn", function (this: HTMLElement) {
    const selection = window.getSelection(),
      range = document.createRange();
    range.selectNodeContents(this);
    selection!.removeAllRanges();
    selection!.addRange(range);
  });
  $panels.on("click", ".embed-howto", function (this: HTMLElement) {
    const url = `${baseUrl()}/embed/${data.game.id}${location.hash}`;
    const iframe =
      '<iframe src="' +
      url +
      '?theme=auto&bg=auto"\nwidth=600 height=397 frameborder=0></iframe>';
    $.modal(
      $(
        '<strong style="font-size:1.5em">' +
          $(this).html() +
          "</strong><br /><br />" +
          "<pre>" +
          li.escapeHtml(iframe) +
          "</pre><br />" +
          iframe +
          "<br /><br />" +
          '<a class="text" data-icon="" href="/developers#embed-game">Read more about embedding games</a>'
      )
    );
  });
}
