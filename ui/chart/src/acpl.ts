import { createBase } from './common';
import { AcplChart, AnalyseData } from './interface';
import Plotly from 'plotly.js-dist-min';

const getAnnotation = (node: Tree.Node) => {
  if (node.glyphs?.some(g => g.id === 4)) return '(Blunder)';
  if (node.glyphs?.some(g => g.id === 2)) return '(Mistake)';
  if (node.glyphs?.some(g => g.id === 6)) return '(Inaccuracy)';
  return '';
};

const makeSeriesData = (d: AnalyseData, mainline: Tree.Node[], trans: Trans) => {
  return mainline.slice(1).flatMap((node, x) => {
    const isWhite = (node.ply & 1) == 1;

    let cp, adv;
    if (node.eval && node.eval.mate) {
      cp = node.eval.mate > 0 ? Infinity : -Infinity;
      adv = '#' + node.eval.mate;
    } else if (node.san?.includes('#')) {
      cp = isWhite ? Infinity : -Infinity;
      if (d.game.variant.key === 'antichess') cp = -cp;
      if (cp < 0) adv = '#-1';
      else adv = '#1';
    } else if (node.eval && typeof node.eval.cp !== 'undefined') {
      cp = node.eval.cp;
      adv = (cp / 100).toFixed(2);
    } else return [];

    const turn = Math.floor((node.ply - 1) / 2) + 1;
    return [
      {
        name: turn + (isWhite ? '. ' : '... ') + node.san + ' ' + getAnnotation(node),
        value: `${trans('advantage')}: ${adv}`,
        x,
        y: 2 / (1 + Math.exp(-0.004 * cp)) - 1,
      },
    ];
  });
};

export default async function (
  el: HTMLElement,
  data: AnalyseData,
  mainline: Tree.Node[],
  trans: Trans
): Promise<AcplChart> {
  const updateData = async (data: AnalyseData, mainline: Tree.Node[], create?: true) => {
    const seriesData = makeSeriesData(data, mainline, trans);
    const values = seriesData.map(n => n.y);

    const toAxes = (points: { x: number; y: number }[], max: number) => ({
      x: points.map(({ x }) => x),
      y: points.map(({ y }) => y / max),
    });

    const min = Math.min(...values, 0);
    const max = Math.max(...values, 0);

    const plotData: Plotly.Data = {
      ...toAxes(seriesData, Math.min(Math.max(-min, max), 1)),
      type: 'scatter',
      line: { width: 2, color: '#48E' },
      hoverinfo: 'x+text',
      hovertext: seriesData.map(({ value }) => value),
    };

    const layout: Partial<Plotly.Layout> = {
      paper_bgcolor: '#0000',
      plot_bgcolor: '#0000',
      margin: { t: 0, b: 0, l: 0, r: 0 },
      showlegend: false,
      hovermode: 'x unified',
      xaxis: {
        fixedrange: true,
        showgrid: false,
        zeroline: false,
        tickmode: 'array',
        tickvals: [...seriesData.keys()],
        ticktext: seriesData.map(({ name }) => name),
        range: [-1, mainline.length],
      },
      yaxis: {
        fixedrange: true,
        showgrid: false,
        range: [-1, 1],
      },
    };

    if (create) await Plotly.newPlot(el, [plotData], layout, { displayModeBar: false, showTips: false });
    else await Plotly.update(el, plotData, layout, 0);
  };

  await updateData(data, mainline, true);

  const { selectPly } = await createBase(el, { division: data.game.division, trans, min: -1, max: 1 });

  return {
    firstPly: data.treeParts[0].ply,
    selectPly,
    updateData,
  };
}
