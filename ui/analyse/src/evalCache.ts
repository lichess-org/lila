import { defined, prop, Prop } from 'common';
import throttle from 'common/throttle';

export interface EvalCache {
  onCeval(): void
  fetch(path: Tree.Path, multiPv: number): void
  onCloudEval(serverEval): void
  upgradable: Prop<boolean>
}

const evalPutMinDepth = 20;
const evalPutMinNodes = 3e6;
const evalPutMaxMoves = 10;

function qualityCheck(ev): boolean {
  // below 500k nodes, the eval might come from an imminent threefold repetition
  // and should therefore be ignored
  return ev.nodes > 500000 && (
    ev.depth >= evalPutMinDepth || ev.nodes > evalPutMinNodes
  );
}

// from client eval to server eval
function toPutData(variant, ev) {
  const data: any = {
    fen: ev.fen,
    knodes: Math.round(ev.nodes / 1000),
    depth: ev.depth,
    pvs: ev.pvs.map(pv => {
      return {
        cp: pv.cp,
        mate: pv.mate,
        moves: pv.moves.slice(0, evalPutMaxMoves).join(' ')
      };
    })
  };
  if (variant !== 'standard') data.variant = variant;
  return data;
}

// from server eval to client eval
function toCeval(e) {
  const res: any = {
    fen: e.fen,
    nodes: e.knodes * 1000,
    depth: e.depth,
    pvs: e.pvs.map(function(from) {
      const to: any = {
        moves: from.moves.split(' ')
      };
      if (defined(from.cp)) to.cp = from.cp;
      else to.mate = from.mate;
      return to;
    }),
    cloud: true
  };
  if (defined(res.pvs[0].cp)) res.cp = res.pvs[0].cp;
  else res.mate = res.pvs[0].mate;
  res.cloud = true;
  return res;
}

export function make(opts): EvalCache {
  const fetchedByFen = {};
  const upgradable = prop(false);
  return {
    onCeval: throttle(500, function() {
      const node = opts.getNode(), ev = node.ceval;
      if (ev && !ev.cloud && node.fen in fetchedByFen && qualityCheck(ev) && opts.canPut()) {
        opts.send('evalPut', toPutData(opts.variant, ev));
      }
    }),
    fetch(path: Tree.Path, multiPv: number): void {
      const node = opts.getNode();
      if ((node.ceval && node.ceval.cloud) || !opts.canGet()) return;
      const serverEval = fetchedByFen[node.fen];
      if (serverEval) return opts.receive(toCeval(serverEval), path);
      else if (node.fen in fetchedByFen) return; // waiting for response
      else fetchedByFen[node.fen] = undefined; // mark as waiting
      const obj: any = {
        fen: node.fen,
        path
      };
      if (opts.variant !== 'standard') obj.variant = opts.variant;
      if (multiPv > 1) obj.mpv = multiPv;
      if (upgradable()) obj.up = true;
      opts.send('evalGet', obj);
    },
    onCloudEval(serverEval) {
      fetchedByFen[serverEval.fen] = serverEval;
      opts.receive(toCeval(serverEval), serverEval.path);
    },
    upgradable,
  };
};
