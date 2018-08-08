import { prop } from 'common';
import { ForecastCtrl, ForecastData, ForecastStep } from './interfaces';
import { AnalyseData } from '../interfaces';
import { countGhosts } from 'draughtsground/fen';

export function make(cfg: ForecastData, data: AnalyseData, redraw: () => void): ForecastCtrl {

    const saveUrl = `/${data.game.id}${data.player.id}/forecasts`;

    function setDisplayPlies(fc?: ForecastStep[][]) {
        if (fc) 
            for (var f = 0; f < fc.length; f++) {
                for (var i = 1; i < fc[f].length; i++) {
                    if (countGhosts(fc[f][i].fen) > 0)
                        fc[f][i].displayPly = fc[f][i].ply + 1;
                }
            }
        return fc;
    }

    let forecasts = setDisplayPlies(cfg.steps) || [];
    const loading = prop(false);

    function keyOf(fc: ForecastStep[]): string {
        return fc.map(node => node.ply + ':' + node.uci).join(',');
    }

    function contains(fc1: ForecastStep[], fc2: ForecastStep[]): boolean {
        return fc1.length >= fc2.length && keyOf(fc1).indexOf(keyOf(fc2)) === 0;
    }

    function findStartingWithNode(node: ForecastStep): ForecastStep[][] {
        return forecasts.filter(function (fc) {
            return contains(fc, [node]);
        });
    }

    function collides(fc1: ForecastStep[], fc2: ForecastStep[]): boolean {
        for (var i = 0, max = Math.min(fc1.length, fc2.length); i < max; i++) {
            if (fc1[i].uci !== fc2[i].uci) {
                if (cfg.onMyTurn) return i !== 0 && i % 2 === 0;
                return i % 2 === 1;
            }
        }
        return true;
    }
     
    function unmergedLength(fc: ForecastStep[]): number {

        if (fc.length <= 1) return fc.length;

        let len = 1;
        for (let i = 1; i < fc.length; i++) {
            const fc1 = (fc[i].displayPly ? fc[i].displayPly : fc[i].ply);
            const fc2 = (fc[i - 1].displayPly ? fc[i - 1].displayPly : fc[i - 1].ply);
            if (fc1 && fc2 && fc1 > fc2)
                len++;
        }

        return len;

    }

    function truncate(fc: ForecastStep[]): ForecastStep[] {
        let fc2 = fc;
        // must end with player move
        if (cfg.onMyTurn) {
            while (fc2.length != 0 && unmergedLength(fc2) % 2 !== 1) {
                fc2 = fc2.slice(0, -1);
            }
        }
        else {
            while (fc2.length != 0 && unmergedLength(fc2) % 2 !== 0) {
                fc2 = fc2.slice(0, -1);
            }
        }
        return fc2.slice(0, 30);
    }

    function truncateNodes(fc: any[]): any[] {
        // must end with player move
        if (cfg.onMyTurn)
            return (fc.length % 2 !== 1 ? fc.slice(0, -1) : fc).slice(0, 30);
        else
            return (fc.length % 2 !== 0 ? fc.slice(0, -1) : fc).slice(0, 30);
    }

    function isLongEnough(fc: ForecastStep[]): boolean {
        return fc.length >= (cfg.onMyTurn ? 1 : 2);
    }

    function fixAll() {
        // remove contained forecasts
        forecasts = forecasts.filter(function (fc, i) {
            return forecasts.filter(function (f, j) {
                return i !== j && contains(f, fc)
            }).length === 0;
        });
        // remove colliding forecasts
        forecasts = forecasts.filter(function (fc, i) {
            return forecasts.filter(function (f, j) {
                return i < j && collides(f, fc)
            }).length === 0;
        });
    }

    fixAll();

    function reloadToLastPly() {
        loading(true);
        redraw();
        history.replaceState(null, '', '#last');
        window.lidraughts.reload();
    };

    function isCandidate(fc: ForecastStep[]): boolean {
        fc = truncate(fc);
        if (!isLongEnough(fc)) return false;
        var collisions = forecasts.filter(function (f) {
            return contains(f, fc);
        });
        if (collisions.length) return false;
        return true;
    };

    function save() {
        if (cfg.onMyTurn) return;
        loading(true);
        redraw();
        $.ajax({
            method: 'POST',
            url: saveUrl,
            data: JSON.stringify(forecasts),
            contentType: 'application/json'
        }).then(function (data) {
            if (data.reload) reloadToLastPly();
            else {
                loading(false);
                forecasts = setDisplayPlies(data.steps) || [];
            }
            redraw();
        });
    };

    function playAndSave(node: ForecastStep) {
        if (!cfg.onMyTurn) return;
        loading(true);
        redraw();
        $.ajax({
            method: 'POST',
            url: saveUrl + '/' + node.uci,
            data: JSON.stringify(findStartingWithNode(node).filter(function (fc) {
                return fc.length > 1;
            }).map(function (fc) {
                return fc.slice(1);
            })),
            contentType: 'application/json'
        }).then(function (data) {
            if (data.reload) reloadToLastPly();
            else {
                loading(false);
                forecasts = setDisplayPlies(data.steps) || [];
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
            forecasts = forecasts.filter((_, i) => i !== index)
            save();
        },
        list: () => forecasts,
        truncate,
        truncateNodes,
        loading,
        onMyTurn: !!cfg.onMyTurn,
        findStartingWithNode,
        playAndSave,
        reloadToLastPly
    };

}
