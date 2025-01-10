import { prop } from 'common/common';
import type { AnalyseData } from '../interfaces';
import type { ForecastCtrl, ForecastData, ForecastStep } from './interfaces';

export function make(cfg: ForecastData, data: AnalyseData, redraw: () => void): ForecastCtrl {
  const saveUrl = `/${data.game.id}${data.player.id}/forecasts`;

  let forecasts = cfg.steps || [];
  const loading = prop(false);

  function keyOf(fc: ForecastStep[]): string {
    return fc.map(node => `${node.ply}:${node.usi}`).join(',');
  }

  function contains(fc1: ForecastStep[], fc2: ForecastStep[]): boolean {
    return fc1.length >= fc2.length && keyOf(fc1).startsWith(keyOf(fc2));
  }

  function findStartingWithNode(node: ForecastStep): ForecastStep[][] {
    return forecasts.filter(fc => contains(fc, [node]));
  }

  function collides(fc1: ForecastStep[], fc2: ForecastStep[]): boolean {
    for (let i = 0, max = Math.min(fc1.length, fc2.length); i < max; i++) {
      if (fc1[i].usi !== fc2[i].usi) {
        if (cfg.onMyTurn) return i !== 0 && i % 2 === 0;
        return i % 2 === 1;
      }
    }
    return true;
  }

  function truncate(fc: ForecastStep[]): ForecastStep[] {
    if (cfg.onMyTurn) return (fc.length % 2 !== 1 ? fc.slice(0, -1) : fc).slice(0, 30);
    // must end with player move
    return (fc.length % 2 !== 0 ? fc.slice(0, -1) : fc).slice(0, 30);
  }

  function isLongEnough(fc: ForecastStep[]): boolean {
    return fc.length >= (cfg.onMyTurn ? 1 : 2);
  }

  function fixAll() {
    // remove contained forecasts
    forecasts = forecasts.filter(
      (fc, i) => forecasts.filter((f, j) => i !== j && contains(f, fc)).length === 0,
    );
    // remove colliding forecasts
    forecasts = forecasts.filter(
      (fc, i) => forecasts.filter((f, j) => i < j && collides(f, fc)).length === 0,
    );
  }

  fixAll();

  function reloadToLastPly() {
    loading(true);
    redraw();
    history.replaceState(null, '', '#last');
    window.lishogi.reload();
  }

  function isCandidate(fc: ForecastStep[]): boolean {
    fc = truncate(fc);
    if (!isLongEnough(fc)) return false;
    const collisions = forecasts.filter(f => contains(f, fc));
    if (collisions.length) return false;
    return true;
  }

  function save() {
    if (cfg.onMyTurn) return;
    loading(true);
    redraw();
    window.lishogi.xhr
      .json('POST', saveUrl, {
        json: forecasts,
      })
      .then(data => {
        if (data.reload) reloadToLastPly();
        else {
          loading(false);
          forecasts = data.steps || [];
        }
        redraw();
      });
  }

  function encodeUsi(usi: string): string {
    return usi.replace(/\+/, '%2B').replace(/=/, '%3D');
  }

  function playAndSave(node: ForecastStep) {
    if (!cfg.onMyTurn) return;
    loading(true);
    redraw();
    window.lishogi.xhr
      .json('POST', `${saveUrl}/${encodeUsi(node.usi)}`, {
        json: findStartingWithNode(node)
          .filter(fc => fc.length > 1)
          .map(fc => fc.slice(1)),
      })
      .then(data => {
        if (data.reload) reloadToLastPly();
        else {
          loading(false);
          forecasts = data.steps || [];
        }
        redraw();
      });
  }

  return {
    addNodes(fc: ForecastStep[]): void {
      fc = truncate(fc);
      if (!isCandidate(fc)) return;
      forecasts.push(fc);
      fixAll();
      save();
    },
    isCandidate,
    removeIndex(index) {
      forecasts = forecasts.filter((_, i) => i !== index);
      save();
    },
    list: () => forecasts,
    truncate,
    loading,
    onMyTurn: !!cfg.onMyTurn,
    findStartingWithNode,
    playAndSave,
    reloadToLastPly,
  };
}
