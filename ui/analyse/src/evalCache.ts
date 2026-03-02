import { defined, prop } from 'lib';
import { throttle } from 'lib/async';
import type { EvalHit, EvalGetData, EvalPutData } from './interfaces';
import type { AnalyseSocketSend } from './socket';
import { pubsub } from 'lib/pubsub';
import type { ClientEval, PvData, ServerEval, TreeNode, TreePath } from 'lib/tree/types';

export interface EvalCacheOpts {
  variant: VariantKey;
  receive(ev: ClientEval, path: TreePath): void;
  send: AnalyseSocketSend;
  getNode(): TreeNode;
  canPut(): boolean;
  canGet(): boolean;
  upgradable: boolean;
}

const evalPutMinDepth = 20;
const evalPutMinNodes = 3e6;
const evalPutMaxMoves = 10;

function qualityCheck(ev: ClientEval): boolean {
  // quick mates may never reach the minimum nodes or depth
  if (Math.abs(ev.mate ?? 99) < 15) return true;
  // below 500k nodes, the eval might come from an imminent threefold repetition
  // and should therefore be ignored
  return ev.nodes > 500000 && (ev.depth >= evalPutMinDepth || ev.nodes > evalPutMinNodes);
}

// from client eval to server eval
function toPutData(variant: VariantKey, ev: ClientEval): EvalPutData {
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
function toCeval(e: ServerEval): ClientEval {
  const res: ClientEval = {
    fen: e.fen,
    nodes: e.knodes * 1000,
    depth: e.depth,
    pvs: e.pvs.map(from => {
      const to: PvData = {
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
  private fetchedByFen: Map<FEN, EvalHit | AwaitingEval> = new Map();
  upgradable = prop(false);

  constructor(readonly opts: EvalCacheOpts) {
    this.upgradable(opts.upgradable);
    pubsub.on('socket.in.crowd', d => this.upgradable(d.nb > 2 && d.nb < 99999));
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

  fetch = (path: TreePath, multiPv: number): void => {
    if (document.visibilityState === 'hidden') return;
    const node = this.opts.getNode();
    if ((node.ceval && node.ceval.cloud) || !this.opts.canGet()) return;
    const fetched = this.fetchedByFen.get(node.fen);
    if (fetched) return this.opts.receive(toCeval(fetched), path);
    else if (fetched === awaitingEval) return;
    const obj: EvalGetData = {
      fen: node.fen,
      path,
    };
    if (this.opts.variant !== 'standard') obj.variant = this.opts.variant;
    if (multiPv > 1) obj.mpv = multiPv;
    if (this.upgradable()) obj.up = true;
    this.fetchThrottled(obj);
  };

  onCloudEval = (ev: EvalHit) => {
    this.fetchedByFen.set(ev.fen, ev);
    this.opts.receive(toCeval(ev), ev.path);
  };

  clear = () => this.fetchedByFen.clear();

  private fetchThrottled = throttle(700, (obj: EvalGetData) => {
    this.fetchedByFen.set(obj.fen, awaitingEval); // waiting for response
    this.opts.send('evalGet', obj);
  });
}
