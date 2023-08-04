import { createBase } from './common';
import { AnalyseData, PlyChart } from './interface';
import Plotly from 'plotly.js-dist-min';

export default async function (
  el: HTMLElement,
  data: AnalyseData,
  trans: Trans,
  hunter: boolean
): Promise<PlyChart | undefined> {
  const { moveCentis } = data.game;
  if (!moveCentis) return; // imported games

  const moveSeries = {
    white: [] as { x: number; y: number }[],
    black: [] as { x: number; y: number }[],
  };
  const totalSeries = {
    white: [] as { x: number; y: number }[],
    black: [] as { x: number; y: number }[],
  };

  const labels: string[] = [];
  const moveLabels = { white: [] as string[], black: [] as string[] };
  const totalLabels = { white: [] as string[], black: [] as string[] };

  const tree = data.treeParts;
  let ply = 0,
    maxMove = 0,
    maxTotal = 0,
    showTotal = !hunter;

  const logC = Math.log(3) ** 2;

  for (const [x, centis] of moveCentis.entries()) {
    const node = tree[x + 1];
    ply = node?.ply ?? ply + 1;
    const san = node?.san;

    const turn = (ply + 1) >> 1;
    const color = ply & 1 ? ('white' as const) : ('black' as const);
    const isWhite = color === 'white';

    const y = Math.pow(Math.log(0.005 * Math.min(centis, 12e4) + 3), 2) - logC;
    maxMove = Math.max(y, maxMove);

    const movePoint = { x, y };

    const seconds = (centis / 100).toFixed(centis >= 200 ? 1 : 2);
    moveSeries[color].push(movePoint);

    if (san) labels.push(turn + (isWhite ? '. ' : '... ') + san);
    else labels.push('');
    moveLabels[color].push(`${color} took ${seconds} secs`);

    let clock = node?.clock;
    if (clock === undefined) {
      if (x < moveCentis.length - 1) showTotal = false;
      else if (data.game.status.name === 'outoftime') clock = 0;
      else if (data.clock) {
        const prevClock = tree[x - 1] ? tree[x - 1].clock : undefined;
        if (prevClock) clock = prevClock + data.clock.increment - centis;
      }
    }
    if (clock !== undefined) {
      totalLabels[color].push(`${formatClock(clock)} left (${color})`);
      maxTotal = Math.max(clock, maxTotal);
      totalSeries[color].push({ x, y: clock });
    }
  }

  const toAxes = (points: { x: number; y: number }[], max: number) => ({
    x: points.map(({ x }) => x),
    y: points.map(({ y }) => y / max),
  });

  await Plotly.newPlot(
    el,
    [
      {
        ...toAxes(moveSeries.white, maxMove),
        type: 'bar',
        marker: { color: '#FFF', line: { width: 1, color: '#666' } },
        hoverinfo: 'x+text',
        hovertext: moveLabels.white,
      },
      {
        ...toAxes(moveSeries.black, maxMove),
        type: 'bar',
        marker: { color: '#222', line: { width: 1, color: '#666' } },
        hoverinfo: 'x+text',
        hovertext: moveLabels.black,
      },
      ...(showTotal
        ? ([
            {
              ...toAxes(totalSeries.white, maxTotal * 1.25),
              type: 'scatter',
              mode: 'lines',
              line: { width: 2 },
              hoverinfo: 'text',
              hovertext: totalLabels.white,
            },
            {
              ...toAxes(totalSeries.black, maxTotal * 1.25),
              type: 'scatter',
              mode: 'lines',
              line: { width: 2 },
              hoverinfo: 'text',
              hovertext: totalLabels.black,
            },
          ] as Plotly.Data[])
        : []),
    ],
    {
      paper_bgcolor: '#0000',
      plot_bgcolor: '#0000',
      margin: { t: 0, b: 0, l: 0, r: 0 },
      showlegend: false,
      hovermode: 'x unified',
      bargap: 0,
      xaxis: {
        fixedrange: true,
        showgrid: false,
        zeroline: false,
        tickmode: 'array',
        tickvals: [...labels.keys()],
        ticktext: labels,
      },
      yaxis: {
        fixedrange: true,
        showgrid: false,
        zeroline: false,
        range: [0, 1],
      },
    },
    {
      displayModeBar: false,
      showTips: false,
    }
  );

  const { selectPly } = await createBase(el, {
    indices: showTotal ? [2, 3] : undefined,
    division: data.game.division,
    trans,
    min: -1,
    max: 1,
  });

  return {
    firstPly: data.treeParts[0].ply,
    selectPly,
  };
}

const formatClock = (centis: number) => {
  let result = '';
  if (centis >= 60 * 60 * 100) result += Math.floor(centis / 60 / 6000) + ':';
  result +=
    Math.floor((centis % (60 * 6000)) / 6000)
      .toString()
      .padStart(2, '0') + ':';
  const secs = (centis % 6000) / 100;
  if (centis < 6000) result += secs.toFixed(2).padStart(5, '0');
  else result += Math.floor(secs).toString().padStart(2, '0');
  return result;
};
