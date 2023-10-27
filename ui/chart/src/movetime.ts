// import divisionLines from './division';
import { AnalyseData } from './interface';

export default async function (_el: HTMLCanvasElement, data: AnalyseData, _trans: Trans, hunter: boolean) {
  const moveCentis = data.game.moveCentis;
  if (!moveCentis) return; // imported games

  const tree = data.treeParts;
  let ply = 0,
    maxMove = 0,
    showTotal = !hunter;

  const logC = Math.pow(Math.log(3), 2);

  const blurs = [toBlurArray(data.player), toBlurArray(data.opponent)];
  if (data.player.color === 'white') blurs.reverse();

  moveCentis.forEach((centis: number, x: number) => {
    const node = tree[x + 1];
    ply = node?.ply ?? ply + 1;
    // const san = node?.san ?? '-';

    // const turn = (ply + 1) >> 1;

    const color = ply & 1;
    // const colorName = color ? 'white' : 'black';

    const y = Math.pow(Math.log(0.005 * Math.min(centis, 12e4) + 3), 2) - logC;
    maxMove = Math.max(y, maxMove);

    if (blurs[color].shift() === '1') {
    }

    // const seconds = (centis / 100).toFixed(centis >= 200 ? 1 : 2);

    let clock = node?.clock;
    if (clock == undefined) {
      if (x < moveCentis.length - 1) showTotal = false;
      else if (data.game.status.name === 'outoftime') clock = 0;
      else if (data.clock) {
        const prevClock = tree[x - 1].clock ?? undefined;
        if (prevClock) clock = prevClock + data.clock.increment - centis;
      }
    }
    console.log(showTotal)

    // lichess.pubsub.on('ply', chart.selectPly);
    // lichess.pubsub.emit('ply.trigger');
    return;
  });
}

const toBlurArray = (player: any) => (player.blurs && player.blurs.bits ? player.blurs.bits.split('') : []);

// const formatClock = (centis: number) => {
//   let result = '';
//   if (centis >= 60 * 60 * 100) result += Math.floor(centis / 60 / 6000) + ':';
//   result +=
//     Math.floor((centis % (60 * 6000)) / 6000)
//       .toString()
//       .padStart(2, '0') + ':';
//   const secs = (centis % 6000) / 100;
//   if (centis < 6000) result += secs.toFixed(2).padStart(5, '0');
//   else result += Math.floor(secs).toString().padStart(2, '0');
//   return result;
// };
