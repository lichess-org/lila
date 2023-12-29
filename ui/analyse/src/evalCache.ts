import { defined, prop } from 'common';
import throttle from 'common/throttle';
import { EvalHit, EvalGetData, EvalPutData } from './interfaces';
import { AnalyseSocketSend } from './socket';

export interface EvalCacheOpts {
  variant: VariantKey;
  receive(ev: Tree.ClientEval, path: Tree.Path): void;
  send: AnalyseSocketSend;
  getNode(): Tree.Node;
  canPut(): boolean;
  canGet(): boolean;
}

const evalPutMinDepth = 20;
const evalPutMinNodes = 3e6;
const evalPutMaxMoves = 10;

function qualityCheck(ev: Tree.ClientEval): boolean {
  // below 500k nodes, the eval might come from an imminent threefold repetition
  // and should therefore be ignored
  return ev.nodes > 500000 && (ev.depth >= evalPutMinDepth || ev.nodes > evalPutMinNodes);
}

// from client eval to server eval
function toPutData(variant: VariantKey, ev: Tree.ClientEval): EvalPutData {
  const data: EvalPutData = {
    fen: ev.fen,
    knodes: Math.round(ev.nodes / 1000),
    depth: ev.depth,
    pvs: ev.pvs.map(pv => {
      return {
        cp: pv.cp,
        mate: pv.mate,
        moves: pv.moves.slice(0, evalPutMaxMoves).join(' '),
      };
    }),
  };
  if (variant !== 'standard') data.variant = variant;
  return data;
}

// from server eval to client eval
function toCeval(e: Tree.ServerEval): Tree.ClientEval {
  const res: Tree.ClientEval = {
    fen: e.fen,
    nodes: e.knodes * 1000,
    depth: e.depth,
    pvs: e.pvs.map(from => {
      const to: Tree.PvData = {
        moves: from.moves.split(' '), // moves come from the server as a single string
      };
      if (defined(from.cp)) to.cp = from.cp;
      else to.mate = from.mate;
      return to;
    }),
    cloud: true,
  };
  if (defined(res.pvs[0].cp)) res.cp = res.pvs[0].cp;
  else res.mate = res.pvs[0].mate;
  res.cloud = true;
  return res;
}

type AwaitingEval = null;
const awaitingEval: AwaitingEval = null;

export default class EvalCache {
  private fetchedByFen: Map<Fen, EvalHit | AwaitingEval> = new Map();
  private upgradable = prop(false);

  constructor(readonly opts: EvalCacheOpts) {
    lichess.pubsub.on('socket.in.crowd', d => this.upgradable(d.nb > 2 && d.nb < 99999));
  }

  onLocalCeval = throttle(500, () => {
    const node = this.opts.getNode(),
      ev = node.ceval;
    const fetched = this.fetchedByFen.get(node.fen);
    if (
      ev &&
      !ev.cloud &&
      this.fetchedByFen.has(node.fen) &&
      (!fetched || fetched.depth < ev.depth) &&
      qualityCheck(ev) &&
      this.opts.canPut()
    ) {
      this.opts.send('evalPut', toPutData(this.opts.variant, ev));
    }
  });

  fetch = (path: Tree.Path, multiPv: number): void => {
    const node = this.opts.getNode();
    if ((node.ceval && node.ceval.cloud) || !this.opts.canGet()) return;
    const fetched = this.fetchedByFen.get(node.fen);
    if (fetched) return this.opts.receive(toCeval(fetched), path);
    else if (fetched === awaitingEval) return;
    // waiting for response
    else this.fetchedByFen.set(node.fen, awaitingEval); // mark as waiting
    const obj: EvalGetData = {
      fen: node.fen,
      path,
    };
    if (this.opts.variant !== 'standard') obj.variant = this.opts.variant;
    if (multiPv > 1) obj.mpv = multiPv;
    if (this.upgradable()) obj.up = true;
    this.opts.send('evalGet', obj);
  };

  onCloudEval = (ev: EvalHit) => {
    this.fetchedByFen.set(ev.fen, ev);
    this.opts.receive(toCeval(ev), ev.path);
  };

  clear = () => this.fetchedByFen.clear();
}
