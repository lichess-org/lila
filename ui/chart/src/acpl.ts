import { loadHighcharts, MovePoint, selectPly } from './common';
import divisionLines from './division';
import { AcplChart, AnalyseData, Player } from './interface';

const acpl: Window['LichessChartGame']['acpl'] = async (
  el: HTMLElement,
  data: AnalyseData,
  mainline: Tree.Node[],
  trans: Trans
) => {
  await loadHighcharts('highchart');

  const area = window.Highcharts.theme.lichess.area;
  const line = window.Highcharts.theme.lichess.line;

  const blurs = [toBlurArray(data.player), toBlurArray(data.opponent)];
  if (data.player.color === 'white') blurs.reverse();

  const makeSerieData = (d: AnalyseData, mainline: Tree.Node[]) => {
    const partial = !d.analysis || d.analysis.partial;
    return mainline.slice(1).map(node => {
      const isWhite = (node.ply & 1) == 1;

      let cp;
      if (node.eval && node.eval.mate) {
        cp = node.eval.mate > 0 ? Infinity : -Infinity;
      } else if (node.san?.includes('#')) {
        cp = isWhite ? Infinity : -Infinity;
        if (d.game.variant.key === 'antichess') cp = -cp;
      } else if (node.eval && typeof node.eval.cp !== 'undefined') {
        cp = node.eval.cp;
      } else return { y: null };

      const turn = Math.floor((node.ply - 1) / 2) + 1;
      const dots = isWhite ? '.' : '...';
      const point: MovePoint = {
        name: turn + dots + ' ' + node.san,
        y: 2 / (1 + Math.exp(-0.004 * cp)) - 1,
      };
      let [annotation, fillColor] = glyphProperties(node);
      const isBlur = !partial && blurs[isWhite ? 1 : 0].shift() === '1';
      if (isBlur) {
        annotation = 'blur';
        fillColor = isWhite ? line.white : line.black;
      }
      if (annotation) {
        point.marker = {
          symbol: isBlur ? 'square' : 'circle',
          radius: 4,
          lineWidth: '1px',
          lineColor: isBlur ? line.accent : fillColor,
          fillColor: fillColor,
        };
        point.name += ` [${annotation}]`;
      }
      return point;
    });
  };

  const disabled = { enabled: false };
  const noText = { text: null };
  const serieData = makeSerieData(data, mainline);
  const chart: AcplChart = window.Highcharts.chart(el, {
    credits: disabled,
    legend: disabled,
    series: [
      {
        name: trans('advantage'),
        data: serieData,
      },
    ],
    chart: {
      type: 'area',
      spacing: [3, 0, 3, 0],
      animation: false,
      events: {
        click(e: any) {
          lichess.pubsub.emit('analysis.chart.click', Math.round(e.xAxis[0].value));
        },
      },
    },
    plotOptions: {
      series: {
        animation: false,
      },
      area: {
        fillColor: area.white,
        negativeFillColor: area.black,
        threshold: 0,
        lineWidth: 1,
        color: line.accent,
        states: {
          hover: {
            lineWidth: 1,
          },
        },
        events: {
          click(e: any) {
            lichess.pubsub.emit('analysis.chart.click', e.point.x);
          },
        },
        marker: {
          radius: 0,
          states: {
            hover: {
              radius: 3,
              lineColor: line.accent,
            },
            select: {
              enabled: false,
            },
          },
        },
      },
    },
    tooltip: {
      pointFormatter: function (this: any, format: string) {
        format = format.replace('{series.name}', trans('advantage'));
        const ev = mainline[this.x + 1].eval;
        if (!ev) return;
        else if (ev.mate) return format.replace('{point.y}', '#' + ev.mate);
        else if (typeof ev.cp !== 'undefined') {
          const e = Math.max(Math.min(Math.round(ev.cp / 10) / 10, 99), -99);
          return format.replace('{point.y}', e > 0 ? `+${e}` : `${e}`);
        }
        return;
      },
    },
    title: noText,
    xAxis: {
      title: noText,
      labels: disabled,
      lineWidth: 0,
      tickWidth: 0,
      plotLines: divisionLines(data.game.division, trans),
    },
    yAxis: {
      title: noText,
      min: -1.1,
      max: 1.1,
      startOnTick: false,
      endOnTick: false,
      labels: disabled,
      lineWidth: 1,
      gridLineWidth: 0,
      plotLines: [
        {
          color: window.Highcharts.theme.lichess.text.weak,
          width: 1,
          value: 0,
        },
      ],
    },
  });
  chart.firstPly = data.treeParts[0].ply;
  chart.selectPly = selectPly.bind(chart);
  chart.updateData = (d: AnalyseData, mainline: Tree.Node[]) =>
    chart.series[0].setData(makeSerieData(d, mainline) as any);

  lichess.pubsub.on('ply', chart.selectPly);
  lichess.pubsub.emit('ply.trigger');

  return chart;
};

// the color prefixes below are mirrored in analyse/src/roundTraining.ts
const glyphProperties = (node: Tree.Node) => {
  const playerAndAlpha = `${node.ply & 1}00`;
  if (node.glyphs?.some(g => g.id == 4)) return ['blunder', '#db303' + playerAndAlpha];
  else if (node.glyphs?.some(g => g.id == 2)) return ['mistake', '#cc9b0' + playerAndAlpha];
  else if (node.glyphs?.some(g => g.id == 6)) return ['inaccuracy', '#1c9ae' + playerAndAlpha];
  else return [undefined, undefined];
};

const toBlurArray = (player: Player) => (player.blurs && player.blurs.bits ? player.blurs.bits.split('') : []);

export default acpl;
