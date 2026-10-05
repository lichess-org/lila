import type { AcplChart, ChartGame } from 'chart';

import { clamp } from 'lib/algo';
import type { CustomSearch } from 'lib/ceval/types';
import { engineSelect, hashSetting, searchTicks } from 'lib/ceval/view/settings';
import { numberFormat } from 'lib/i18n';
import { licon } from 'lib/licon';
import { log } from 'lib/permalog';
import { pubsub } from 'lib/pubsub';
import { storedIntProp, storedStringProp, type StoredProp } from 'lib/storage';
import {
  type Dialog,
  type LooseVNodes,
  alert,
  confirm,
  jsx,
  jsxDialog,
  onInsert,
  rangeConfig,
  spinnerVdom,
} from 'lib/view';
import { text as xhrText } from 'lib/xhr';

import { isFinished } from '@/study/studyChapters';

import type AnalyseCtrl from '../ctrl';
import type { AnalysisEngineInfo } from '../interfaces';
import { LocalAnalysisEngine } from './localAnalysisEngine';

export async function localAnalysisDialog(ctrl: AnalyseCtrl): Promise<void> {
  const state = new LocalAnalysisDialog(ctrl, await site.asset.loadEsm<ChartGame>('chart.game'));
  jsxDialog({
    class: 'local-analysis-dialog',
    css: [{ hashed: 'analyse.local-dialog' }],
    modal: true,
    onClose: state.close,
    render: state.render,
  });
}

class LocalAnalysisDialog {
  private engine?: LocalAnalysisEngine;
  private chartData: Parameters<ChartGame['acpl']>[1];
  private chart?: AcplChart;
  private status?: LooseVNodes;
  private readonly quality = storedIntProp('local-analysis.quality', 0);
  private readonly engineId: StoredProp<string>;
  private readonly threads: StoredProp<number>;
  private readonly hashSize: StoredProp<number>;
  private updateDownloadStatus: (d: { bytes: number; total: number }) => void;

  constructor(
    readonly ctrl: AnalyseCtrl,
    private readonly chartGame: ChartGame,
  ) {
    const info = ctrl.ceval.info()!;
    this.engineId = storedStringProp('local-analysis.engine', info.engine.id);
    this.threads = storedIntProp('local-analysis.threads', info.threads);
    this.hashSize = storedIntProp('local-analysis.hash', info.hashSize);
    this.engine = new LocalAnalysisEngine(ctrl);
  }

  readonly render = (redraw: Redraw, dialog: Dialog): LooseVNodes => {
    if (!this.updateDownloadStatus) {
      this.updateDownloadStatus = (d: { bytes: number; total: number }) => {
        const downloadStatus = i18n.localAnalysis.downloadingXofY(
          Math.round((d.bytes * 100) / d.total) + '%',
          Math.round(d.total / 1000 / 1000) + 'MB',
        );
        if (downloadStatus === this.status) return;

        this.status = downloadStatus;
        redraw();
      };
      pubsub.on('ceval.engine.download', this.updateDownloadStatus);
    }

    return [
      <div class="main-content">
        <h2>{i18n.study.analysisEditor}</h2>
        <div class={['analysis-info', !this.canAnalyse && 'hidden', !this.engine && 'none']}>
          {this.analysisEditor(redraw)}
        </div>
        <div class={['chart-container', this.canAnalyse && 'none']}>
          <canvas
            class="chart"
            hook={onInsert<HTMLCanvasElement>(async canvas => {
              this.chart = await this.chartGame.acpl(canvas, this.ctrl.data, this.engine!.nodes);
            })}
          />
        </div>
        <div class={['working', this.isIdle && 'none']}>
          {spinnerVdom()}
          <span>{i18n.localAnalysis.keepThisBrowserTabActive}</span>
        </div>
      </div>,
      <span class="footer">
        <button
          class={['button button-empty button-red cancel-btn', this.isIdle && 'none']}
          on={{ click: () => dialog.close('cancel') }}>
          {i18n.site.cancel}
        </button>
        <button
          class={[
            'button button-empty button-clas publish-btn',
            !(this.isIdle && this.canPublish.showButton) && 'none',
          ]}
          on={{ click: async () => this.clickPublish().then(redraw) }}>
          {i18n.localAnalysis.publish}
        </button>
        <p class="status">
          {this.status ?? (this.canPublish.showButton && i18n.localAnalysis.youCanPublish)}
        </p>
        <button
          class={['button analyse-btn', !this.canAnalyse && 'none']}
          on={{ click: async () => this.analyse(dialog, redraw) }}>
          {i18n.localAnalysis.analyse}
        </button>
        <button class={['button ok-btn', this.engine && 'none']} on={{ click: () => dialog.close('ok') }}>
          {i18n.site.ok}
        </button>
      </span>,
    ];
  };

  readonly close = () => {
    pubsub.off('ceval.engine.download', this.updateDownloadStatus);
    this.engine?.stop();
  };

  private async analyse(dlg: Dialog, redraw: Redraw): Promise<void> {
    const then = performance.now();
    const engine = this.engine!;
    try {
      const division = await engine.getDivision();
      this.chartData = {
        ...this.ctrl.data,
        game: { ...this.ctrl.data.game, division },
        analysis: { partial: true },
      };
      const result = await engine.analyse(
        this.customSearch,
        division,
        (moves: number, totalMoves: number, nodesPerMove: number) => {
          this.updateEngineStatus(moves, totalMoves, nodesPerMove);
          this.chart?.updateData(this.chartData, engine.nodes);
          redraw();
        },
      );
      await this.ctrl.idbTree.saveAnalysis(result);
      this.status =
        i18n.site.done + ' ' + i18n.site.nbSeconds(Math.round((performance.now() - then) / 100) / 10);
      this.ctrl.mergeLocalAnalysisData(result.localUpdate);
      this.engine = undefined;
      redraw();
    } catch (e) {
      if (e !== 'cancelled') {
        log(e);
        await alert(String(e));
      }
      dlg.close('cancel');
    }
  }

  private async clickPublish() {
    if (this.canPublish.whyNot) {
      return alert(this.canPublish.whyNot);
    }
    if (
      this.ctrl.study &&
      !this.ctrl.study.canMergeAnalysisCleanly() &&
      !(await confirm(i18n.localAnalysis.whenUpgradingOldChapters, i18n.localAnalysis.publish))
    ) {
      return;
    }
    const serverDoc = await this.ctrl.idbTree.serverDocument();
    if (!serverDoc) {
      log(`localAnalysisDialog: getVerified failed for ${this.ctrl.idbTree.id}`);
      this.status = i18n.localAnalysis.analysisUploadFailed;
      return this.ctrl.idbTree.clear('analysis');
    }
    const rsp = await fetch('/analysis/publish', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(serverDoc),
    });

    if (rsp.status === 423) {
      return alert(i18n.localAnalysis.serverAnalysisInProgress);
    } else if (!rsp.ok) {
      log(`${rsp.status} ${rsp.statusText} ${(await rsp.text()).slice(0, 255)}`);
      this.status = i18n.localAnalysis.analysisUploadFailed;
    } else {
      this.ctrl.publishedEvalEngine = structuredClone(this.ctrl.staticAnalysis?.engine);
      await this.ctrl.idbTree.clear('analysis');
      this.ctrl.redraw();
      this.status = i18n.site.success;
    }
  }

  private readonly clickClearLocal = async () => {
    if (await confirm(i18n.study.clearLocal)) {
      await this.ctrl.idbTree.clear('analysis');
      site.reload();
    }
  };

  private readonly clickClearPublished = async () => {
    if (!this.ctrl.opts.study || !(await confirm(i18n.study.clearPublished))) return;
    try {
      await xhrText(`/analysis/${this.ctrl.opts.study.id}/${this.ctrl.opts.study.chapter.id}`, {
        method: 'DELETE',
      });
      site.reload();
    } catch (e) {
      await alert(String(e));
    }
  };

  private readonly updateEngineStatus = (nodeIndex: number, totalNodes: number, nodesPerMove: number) => {
    const progress =
      nodeIndex === 0
        ? i18n.localAnalysis.startingPosition
        : i18n.localAnalysis.moveXOfY(nodeIndex, totalNodes - 1);
    const efficiency = this.timedEngineNodeEfficiency;
    if (isFinite(nodesPerMove) && efficiency) {
      nodesPerMove *= efficiency;
      const val = nodesPerMove / 1_000_000;
      if (val > 1) {
        this.status = [
          progress,
          <br />,
          `(${i18n.localAnalysis.xTimesFishnetQuality(
            val < 5 ? Math.round(10 * val) / 10 : Math.round(val),
          )})`,
        ];
        return;
      }
    }
    this.status = progress;
  };

  private analysisEditor(redraw: Redraw) {
    const info = this.ctrl.ceval.info(this.customSearch)!;
    const ceval = this.ctrl.ceval;
    const [current, published] = [
      this.ctrl.staticAnalysis?.engine,
      this.ctrl.idbTree.hasLocalAnalysis && this.ctrl.publishedEvalEngine,
    ].filter(Boolean);
    const change =
      <Value,>(prop: StoredProp<Value>) =>
      (value: Value) => {
        prop(value);
        redraw();
      };
    return [
      published && [
        this.separator(i18n.localAnalysis.onTheServer),
        this.analysisInfo(this.ctrl.publishedEvalEngine),
        this.separator(i18n.localAnalysis.currentAnalysis),
      ],
      current && this.analysisInfo(this.ctrl.staticAnalysis?.engine),
      (published || current) && this.separator(i18n.localAnalysis.willUse),
      <div class="analysis-settings">
        <div class="setting">
          <label for="local-analysis-engine">Engine:</label>
          {engineSelect(
            ceval.engines.supporting({ rules: ceval.rules, nonStandardMaterial: ceval.nonStandardMaterial }),
            info.engine.id,
            change(this.engineId),
            'local-analysis-engine',
          )}
        </div>
        {this.searchQualitySetting(redraw)}
        <div class="setting">
          <label for="local-analysis-threads">{i18n.site.threads}</label>
          <input
            id="local-analysis-threads"
            type="range"
            min={info.engine.minThreads}
            max={info.engine.maxThreads}
            step={1}
            disabled={info.engine.minThreads === info.engine.maxThreads}
            hook={rangeConfig(() => info.threads, change(this.threads))}
          />
          <div class="range_value">
            {info.threads} / {info.engine.maxThreads}
          </div>
        </div>
        {hashSetting(info.engine, info.hashSize, change(this.hashSize), 'local-analysis-hash', 'Hash')}
      </div>,
      <hr class="separator" />,
      this.estimates(),
    ];
  }

  private analysisInfo(info: AnalysisEngineInfo | undefined): LooseVNodes {
    if (!info) return false;
    const isPublished = info === this.ctrl.publishedEvalEngine;
    const isLocal = this.ctrl.idbTree.hasLocalAnalysis && info !== this.ctrl.publishedEvalEngine;
    const splitVersion = info.engineVersion.split('/');
    const isFishnet = splitVersion.length === 3;
    const [clearText, clearClick] = isLocal
      ? [i18n.study.clearLocal, this.clickClearLocal]
      : [i18n.study.clearPublished, this.clickClearPublished];
    const quality =
      info.nodesPerMove === 1_000_000
        ? i18n.site.standard
        : (() => {
            const efficiency = this.ctrl.ceval.engines.nodeEfficiencyVsFishnet(info.id);
            if (!efficiency) return '';
            return i18n.localAnalysis.xTimesFishnetQuality(
              Math.round((efficiency * info.nodesPerMove) / 100_000) / 10,
            );
          })();
    const provenance = isFishnet
      ? 'fishnet'
      : isLocal
        ? i18n.localAnalysis.local.toLowerCase()
        : i18n.site.by(info.userId);
    return [
      <label>{isPublished ? i18n.localAnalysis.published : i18n.localAnalysis.using}</label>,
      <p class="span-three">
        {provenance}
        <span class="weak">
          {isFishnet ? splitVersion[1] : info.engineVersion}
          {(isLocal || this.ctrl.study?.members.canContribute()) && (
            <button class="clear" title={clearText} on={{ click: clearClick }}>
              {licon.X}
            </button>
          )}
        </span>
      </p>,
      quality && [<label>{i18n.localAnalysis.quality}:</label>, <p>{quality}</p>],
      [<label>{i18n.localAnalysis.nodesPerMove}:</label>, <p>{numberFormat(info.nodesPerMove)}</p>],
    ];
  }

  private searchQualitySetting(redraw: Redraw) {
    const ticks = [0, ...searchTicks.filter(Number.isFinite)];
    const getTick = () =>
      clamp(
        ticks.findIndex(seconds => seconds * 1000 >= this.quality()),
        { min: 0, max: ticks.length - 1 },
      );
    const seconds = ticks[getTick()];
    const value = seconds === 0 ? i18n.site.standard : `${seconds}s`;
    return (
      <div class="setting" title={i18n.site.searchTimeDescription}>
        <label for="local-analysis-quality">{i18n.localAnalysis.quality}</label>
        <input
          id="local-analysis-quality"
          type="range"
          max={ticks.length - 1}
          aria-valuetext={seconds === 0 ? value : i18n.site.nbSeconds(seconds)}
          hook={rangeConfig(getTick, index => {
            this.quality(ticks[index] * 1000);
            redraw();
          })}
        />
        <div class="range_value">{value}</div>
      </div>
    );
  }

  private estimates() {
    const estimates: string[] = [];
    if (this.timeToComplete) {
      const seconds = Math.ceil(this.timeToComplete);
      const minutes = Math.ceil(this.timeToComplete / 60);
      estimates.push(
        i18n.localAnalysis.timeToComplete(
          minutes > 1 ? i18n.site.nbMinutes(minutes) : i18n.site.nbSeconds(seconds),
        ),
      );
    }
    if (this.strengthVsStandard) {
      const val = Math.round(this.strengthVsStandard * 10) / 10;
      estimates.push(i18n.localAnalysis.xTimesFishnetQuality(val > 10 ? Math.round(val) : val));
    }
    return <span>{estimates.join('. ')}</span>;
  }

  private separator(label: string) {
    return (
      <div class="separator">
        <hr></hr>
        {label}
        <hr></hr>
      </div>
    );
  }

  private get isIdle() {
    return !this.engine?.busy;
  }

  private get canAnalyse() {
    return this.engine?.busy === false;
  }

  private get canPublish() {
    const ctrl = this.ctrl;
    if (!this.ctrl.idbTree.hasLocalAnalysis) return { showButton: false };
    if (!this.isIdle || !ctrl.canAnalyse() || !ctrl.allowLines()) return { showButton: false };
    if (ctrl.mainline.length < 10 || !ctrl.ceval.analysable) return { showButton: false, whyNot: 'invalid' };
    if (!ctrl.study || !ctrl.study.members.canContribute())
      return { showButton: false, whyNot: 'permission' };
    if (!ctrl.study.vm.mode.write) return { showButton: true, whyNot: i18n.localAnalysis.turnOnRec };
    if (ctrl.study.relay && !isFinished(ctrl.study.data.chapter))
      return { showButton: false, whyNot: 'ongoing' };
    return { showButton: true };
  }

  private get timedEngineNodeEfficiency(): number | undefined {
    const flavor = this.ctrl.ceval.rules === 'chess' ? 'chess' : 'variant';
    return this.ctrl.ceval.engines
      .supporting({
        rules: this.ctrl.ceval.rules,
        nonStandardMaterial: this.ctrl.ceval.nonStandardMaterial,
      })
      .find(engine => engine.id === this.engineId())?.nodeEfficiencyVsFishnet?.[flavor];
  }

  private standardQualityNodesAt(nodes = 1_000_000): number {
    const threadDilution = 1 + (this.threads() / 32) * (1_000_000 / nodes);
    return Math.round((threadDilution * nodes) / (this.timedEngineNodeEfficiency ?? 1));
  }

  private get timeToComplete(): number | undefined {
    const info = this.ctrl.ceval.info(this.customSearch);
    if (!info) return undefined;
    if ('movetime' in info.search.by) return (info.search.by.movetime * this.ctrl.mainline.length) / 1000;
    const nodesPerSecond = this.ctrl.ceval.nodesPerSecond(info.engine.id, info.threads);
    if (!nodesPerSecond || !this.timedEngineNodeEfficiency) return undefined;
    return (this.standardQualityNodesAt() * this.ctrl.mainline.length) / nodesPerSecond;
  }

  private get strengthVsStandard(): number | undefined {
    const info = this.ctrl.ceval.info(this.customSearch);
    if (!info || !('movetime' in info.search.by) || !this.timedEngineNodeEfficiency) return undefined;
    const nodesPerSecond = this.ctrl.ceval.nodesPerSecond(info.engine.id, info.threads);
    if (!nodesPerSecond) return undefined;
    return this.standardQualityNodesAt((info.search.by.movetime / 1000) * nodesPerSecond) / 1_000_000;
  }

  private get customSearch(): CustomSearch {
    const { engines, rules, nonStandardMaterial } = this.ctrl.ceval;
    const engine =
      engines.supporting({ rules, nonStandardMaterial }).find(engine => engine.id === this.engineId()) ??
      engines.active()!;
    return {
      engine: { id: engine.id, threads: this.threads(), hashSize: this.hashSize() },
      search: () => ({
        by:
          this.quality() === 0
            ? { nodes: this.standardQualityNodesAt() }
            : { movetime: Math.min(this.quality(), engine.maxMovetime ?? 300_000) },
        multiPv: 1,
      }),
      canBackground: true,
    };
  }
}
