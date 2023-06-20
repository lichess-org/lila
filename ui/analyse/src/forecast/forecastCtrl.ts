import { prop, notEmpty, Prop } from 'common';
import * as xhr from 'common/xhr';
import { ForecastData, ForecastList, ForecastStep } from './interfaces';
import { AnalyseData } from '../interfaces';
import { scalachessCharPair } from 'chessops/compat';
import { parseUci } from 'chessops';
import { TreeWrapper } from 'tree';

export default class ForecastCtrl {
  forecasts: Prop<ForecastList> = prop<ForecastList>([]);
  loading = prop(false);

  constructor(readonly cfg: ForecastData, readonly data: AnalyseData, readonly redraw: () => void) {
    this.forecasts(cfg.steps || []);
    this.fixAll();
  }

  private saveUrl = () => `/${this.data.game.id}${this.data.player.id}/forecasts`;

  private keyOf = (fc: ForecastStep[]): string => fc.map(node => node.ply + ':' + node.uci).join(',');

  private update = (f: (fc: ForecastList) => ForecastList) => this.forecasts(f(this.forecasts()));

  contains = (fc1: ForecastStep[], fc2: ForecastStep[]): boolean =>
    fc1.length >= fc2.length && this.keyOf(fc1).startsWith(this.keyOf(fc2));

  findStartingWithNode = (node: ForecastStep): ForecastStep[][] =>
    this.update(fc => fc.filter(fc => this.contains(fc, [node])));

  collides = (fc1: ForecastStep[], fc2: ForecastStep[]): boolean => {
    for (let i = 0, max = Math.min(fc1.length, fc2.length); i < max; i++) {
      if (fc1[i].uci !== fc2[i].uci) {
        return this.cfg.onMyTurn ? i !== 0 && i % 2 === 0 : i % 2 === 1;
      }
    }
    return true;
  };

  truncate = (fc: ForecastStep[]): ForecastStep[] =>
    this.cfg.onMyTurn
      ? (fc.length % 2 !== 1 ? fc.slice(0, -1) : fc).slice(0, 30)
      : // must end with player move
        (fc.length % 2 !== 0 ? fc.slice(0, -1) : fc).slice(0, 30);

  isLongEnough = (fc: ForecastStep[]): boolean => fc.length >= (this.cfg.onMyTurn ? 1 : 2);

  fixAll = () => {
    // remove contained forecasts
    this.update(fcs =>
      fcs.filter((fc, i) => fcs.filter((f, j) => i !== j && this.contains(f, fc)).length === 0)
    );
    // remove colliding forecasts
    this.update(fcs =>
      fcs.filter((fc, i) => fcs.filter((f, j) => i < j && this.collides(f, fc)).length === 0)
    );
  };

  reloadToLastPly = () => {
    this.loading(true);
    this.redraw();
    history.replaceState(null, '', '#last');
    lichess.reload();
  };

  isCandidate = (fc: ForecastStep[]): boolean => {
    fc = this.truncate(fc);
    if (!this.isLongEnough(fc)) return false;
    return !this.forecasts().find(f => this.contains(f, fc));
  };

  save = () => {
    if (this.cfg.onMyTurn) return;
    this.loading(true);
    this.redraw();
    xhr
      .json(this.saveUrl(), {
        method: 'POST',
        body: JSON.stringify(this.forecasts()),
        headers: { 'Content-Type': 'application/json' },
      })
      .then(data => {
        if (data.reload) this.reloadToLastPly();
        else {
          this.loading(false);
          this.forecasts(data.steps || []);
        }
        this.redraw();
      });
  };

  playAndSave = (node: ForecastStep) => {
    if (!this.cfg.onMyTurn) return;
    this.loading(true);
    this.redraw();
    xhr
      .json(`${this.saveUrl()}/${node.uci}`, {
        method: 'POST',
        body: JSON.stringify(
          this.findStartingWithNode(node)
            .filter(notEmpty)
            .map(fc => fc.slice(1))
        ),
        headers: { 'Content-Type': 'application/json' },
      })
      .then(data => {
        if (data.reload) this.reloadToLastPly();
        else {
          this.loading(false);
          this.forecasts(data.steps || []);
        }
        this.redraw();
      });
  };

  showForecast = (path: string, tree: TreeWrapper, nodes: ForecastStep[]) => {
    nodes.forEach(node => {
      const moveId = scalachessCharPair(parseUci(node.uci)!);

      // this handles the case where the move isn't in the tree yet
      // if it is, it just returns
      tree.addNode(
        {
          ply: node.ply,
          fen: node.fen,
          uci: node.uci,
          san: node.san,
          id: moveId,
          children: [],
        },
        path // the path before this is its parent
      );
      path += moveId;
    });
    return path;
  };

  addNodes = (fc: ForecastStep[]): void => {
    fc = this.truncate(fc);
    if (!this.isCandidate(fc)) return;
    this.update(fcs => [...fcs, fc]);
    this.fixAll();
    this.save();
  };
  removeIndex = (index: number) => {
    this.update(fcs => fcs.filter((_, i) => i !== index));
    this.save();
  };
  onMyTurn = () => !!this.cfg.onMyTurn;
}
