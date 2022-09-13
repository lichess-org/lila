import { ChartElm, loadHighcharts, MovePoint } from './common';
import divisionLines from './division';

// we share these same incomplete colors with analyse/roundTraining.ts
export const ErrorColors = {
  // only 5 hex digits.  append '0' for black or '1' for white
  inaccuracy: '#1c9ae',
  blunder: '#db303',
  mistake: '#cc9b0',
};

const acpl: Window['LichessChartGame']['acpl'] = async (
  data: any,
  mainline: Tree.Node[],
  trans: Trans,
  el: ChartElm,
  isHunter: boolean
) => {
  await loadHighcharts('highchart');
  acpl.update = (d: any, mainline: Tree.Node[]) =>
    el.highcharts && el.highcharts.series[0].setData(makeSerieData(d, mainline));

  const area = window.Highcharts.theme.lichess.area;
  const line = window.Highcharts.theme.lichess.line;

  const blurs = [toBlurArray(data.player), toBlurArray(data.opponent)];
  if (data.player.color === 'white') blurs.reverse();

  const makeSerieData = (d: any, mainline: Tree.Node[]) => {
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
      let [annotation, markColor] = glyphProperties(node.glyphs);
      markColor += isWhite ? '1' : '0';

      const isBlur = !partial && blurs[isWhite ? 1 : 0].shift() === '1';
      if (isBlur) {
        annotation = 'blur';
        markColor = isWhite ? line.white : line.black;
      }
      if (annotation && (!isHunter || isBlur)) {
        point.marker = {
          symbol: isBlur ? 'square' : 'circle',
          radius: isBlur ? 4 : 3,
          lineWidth: '1px',
          lineColor: isBlur ? line.accent : markColor,
          fillColor: markColor,
        };
        point.name += ` [${annotation}]`;
      }
      return point;
    });
  };

  const disabled = { enabled: false };
  const noText = { text: null };
  const serieData = makeSerieData(data, mainline);
  el.highcharts = window.Highcharts.chart(el, {
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
        fillColor: isHunter ? area.white : area.acplWhite,
        negativeFillColor: isHunter ? area.black : area.acplBlack,
        threshold: 0,
        lineWidth: 1,
        color: line.grey,
        states: {
          hover: {
            lineWidthPlus: 0,
          },
        },
        events: {
          click(e: any) {
            lichess.pubsub.emit('analysis.chart.click', e.point.x);
          },
        },
        marker: {
          radius: isHunter ? 1 : 0,
          states: {
            hover: {
              radius: 3,
              lineColor: isHunter ? line.accent : line.grey,
            },
            select: {
              enabled: isHunter,
              radius: 4,
              lineColor: line.accent,
              fillColor: line.accent,
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
      plotLines: divisionLines(data.game.division, trans, isHunter),
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
  el.highcharts.selectPly = isHunter
    ? (ply: number) => {
        const point = el.highcharts.series[0].data[ply - 1 - data.game.startedAtTurn];
        if (point) point.select(true);
        else el.highcharts.getSelectedPoints().forEach((point: any) => point.select(false));
      }
    : (ply: number) => {
        const plyline = window.Highcharts.charts[0].xAxis[0].plotLinesAndBands[0];
        plyline.options.value = ply - 1 - (data.game.startedAtTurn || 0);
        plyline.render(); // undocumented but...  i'm sure it's fine ;p
      };
  lichess.pubsub.emit('analysis.change.trigger');
};

const glyphProperties = (glyphs: Array<Tree.Glyph> | undefined) => {
  if (glyphs?.some(g => g.id == 4)) return ['blunder', ErrorColors.blunder];
  else if (glyphs?.some(g => g.id == 2)) return ['mistake', ErrorColors.mistake];
  else if (glyphs?.some(g => g.id == 6)) return ['inaccuracy', ErrorColors.inaccuracy];
  else return [undefined, undefined];
};

const toBlurArray = (player: any) => (player.blurs && player.blurs.bits ? player.blurs.bits.split('') : []);

export default acpl;
