import type { AcplChart, ChartGame } from 'chart';

import type { CustomSearch, EngineInfo } from 'lib/ceval/types';
import { numberFormat } from 'lib/i18n';
import { licon } from 'lib/licon';
import { log } from 'lib/permalog';
import {
  type Dialog,
  type LooseVNodes,
  alert,
  confirm,
  jsx,
  jsxDialog,
  onInsert,
  spinnerVdom,
} from 'lib/view';
import { text as xhrText } from 'lib/xhr';

import { isFinished } from '@/study/studyChapters';

import type AnalyseCtrl from '../ctrl';
import { LocalAnalysisEngine } from './localAnalysisEngine';

type Preset = 'standard' | 'broadcast' | 'timed';

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
  private mode: { preset: Preset; customSearch: CustomSearch };
  private readonly storageKey = 'analyse.local.preset';
  private readonly presets: Record<Preset, { label: string; nodes?: number; title: () => string }>;

  constructor(
    readonly ctrl: AnalyseCtrl,
    private readonly chartGame: ChartGame,
  ) {
    this.engine = new LocalAnalysisEngine(ctrl);
    this.presets = {
      standard: {
        label: i18n.site.standard,
        nodes: 1_000_000,
        title: () => i18n.localAnalysis.standardQuality,
      },
      broadcast: {
        label: i18n.localAnalysis.broadcast,
        nodes: 5_000_000,
        title: () => i18n.localAnalysis.broadcastQuality,
      },
      timed: {
        label: i18n.localAnalysis.timed,
        title: () => {
          const sentences = [i18n.localAnalysis.timedQuality];
          const efficiency = this.timedEngineNodeEfficiency();
          if (efficiency) {
            const ceval = this.ctrl.ceval;
            const { threads, engine } = ceval.info()!;
            const nps = ceval.nodesPerSecond(engine.id, threads);
            if (nps) {
              let tick: number;
              for (tick = 2; efficiency * nps * tick <= 5_000_000; tick += 2) {}
              sentences.push(i18n.localAnalysis.outperformBroadcastXSeconds(tick));
            }
          }
          return sentences.join(' ');
        },
      },
    };
    this.selectPreset();
  }

  readonly render = (redraw: Redraw, dialog: Dialog): LooseVNodes => {
    const analysedNodes =
      this.localNpm && this.publishedNpm
        ? Math.max(this.localNpm, this.publishedNpm)
        : this.localNpm || this.publishedNpm;

    return [
      <div class="preset-tabs">
        <label>Quality:</label>
        {Object.entries(this.presets)
          .filter(([preset]) => this.getMode(preset as Preset))
          .map(([preset, info]) => (
            <button
              class={[
                'preset-tab',
                preset === this.mode.preset && 'active',
                Number(info.nodes) <= analysedNodes && 'checked',
                this.engine?.busy !== false && preset !== this.mode.preset && 'none',
              ]}
              title={info.title()}
              on={{
                click: () => {
                  this.clickPreset(preset as Preset);
                  redraw();
                },
              }}>
              {info.label}
            </button>
          ))}
      </div>,
      <div class="main-content">
        <div class={['preset-infos', !this.canAnalyse && 'hidden', !this.engine && 'none']}>
          {(Object.keys(this.presets) as Preset[]).map(this.presetInfo)}
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
        this.mode.customSearch,
        division,
        (moves: number, totalMoves: number, nodesPerMove: number) => {
          this.updateEngineStatus(moves, totalMoves, nodesPerMove);
          this.chart?.updateData(this.chartData, engine.nodes);
          redraw();
        },
      );
      await this.ctrl.idbTree.saveAnalysis(result);
      this.status = i18n.localAnalysis.doneInX(((performance.now() - then) / 1000).toFixed(1));
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
      this.publishedNpm &&
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

  private clickPreset(preset: Preset) {
    localStorage.setItem(this.storageKey, preset);
    this.selectPreset(preset);
    this.status = undefined;
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

  private selectPreset(preset?: Preset) {
    if (!preset) {
      const nodesToBeat = Math.max(this.localNpm, this.publishedNpm);
      preset = localStorage.getItem(this.storageKey) as Preset;
      if (!(preset in this.presets)) preset = 'standard';
      if (Number(this.presets[preset].nodes) < nodesToBeat)
        preset = nodesToBeat < this.presets.broadcast.nodes! ? 'broadcast' : 'timed';
    }
    this.mode = this.getMode(preset) ?? this.getMode('timed')!;
  }

  private readonly updateEngineStatus = (nodeIndex: number, totalNodes: number, nodesPerMove: number) => {
    const progress =
      nodeIndex === 0
        ? i18n.localAnalysis.startingPosition
        : i18n.localAnalysis.moveXOfY(nodeIndex, totalNodes - 1);
    const efficiency = this.timedEngineNodeEfficiency();
    if (this.mode.preset === 'timed' && isFinite(nodesPerMove) && efficiency) {
      nodesPerMove *= efficiency;
      for (const fasterThan of [this.presets.broadcast, this.presets.standard]) {
        const multiplier = nodesPerMove / fasterThan.nodes!;
        if (multiplier <= 1) continue;
        this.status = [
          progress,
          <br />,
          `(${i18n.localAnalysis.xTimesYQuality(
            multiplier < 5 ? Math.round(10 * multiplier) / 10 : Math.round(multiplier),
            fasterThan.label.toLocaleLowerCase(),
          )})`,
        ];
        return;
      }
    }
    this.status = progress;
  };

  private readonly presetInfo = (preset: Preset) => {
    const mode = this.getMode(preset);
    if (!mode) return false;
    const info = this.ctrl.ceval.info(mode.customSearch)!;
    const param = (label: string, value: string, cls = '') => [
      <label>{label}:</label>,
      cls ? <p class={cls}>{value}</p> : <p>{value}</p>,
    ];
    const nps = this.ctrl.ceval.nodesPerSecond(info.engine.id, info.threads) || 0;
    const efficiency = this.timedEngineNodeEfficiency() || 0;
    const projectedQuality =
      'movetime' in info.search.by &&
      efficiency > 0 &&
      nps > 0 &&
      this.ctrl.ceval.rules === 'chess' &&
      (() => {
        let multiplier = Math.round((info.search.by.movetime * nps * efficiency) / 100_000_000) / 10;
        if (multiplier > 10) multiplier = Math.round(multiplier);
        return param(
          i18n.localAnalysis.projected,
          i18n.localAnalysis.xTimesYQuality(multiplier, i18n.site.standard.toLocaleLowerCase()),
          'span-three',
        );
      })();
    const searchParam =
      'movetime' in info.search.by
        ? param(i18n.site.time, i18n.site.nbSeconds(Math.round(info.search.by.movetime / 1000)))
        : 'nodes' in info.search.by &&
          param(i18n.localAnalysis.nodesPerMove, numberFormat(info.search.by.nodes));

    return (
      <div class={['preset-info', preset !== this.mode.preset && 'none']}>
        {this.ctrl.idbTree.hasLocalAnalysis && this.ctrl.publishedEvalEngine ? (
          this.analysisInfo(this.ctrl.publishedEvalEngine)
        ) : (
          <span>{this.presets[preset].title()}</span>
        )}
        {this.analysisInfo()}
        {this.separator(i18n.localAnalysis.XAnalysis(this.presets[preset].label))}
        {param(i18n.localAnalysis.willUse, info.engine.name ?? '', 'weak')}
        {searchParam}
        {projectedQuality}
      </div>
    );
  };

  private analysisInfo(info = this.ctrl.staticAnalysis?.engine): LooseVNodes {
    if (!info) return false;
    const isTitlePane = this.ctrl.idbTree.hasLocalAnalysis && info === this.ctrl.publishedEvalEngine;
    const isLocalPane = this.ctrl.idbTree.hasLocalAnalysis && !isTitlePane;
    const splitVersion = info.engineVersion.split('/');
    const engine =
      splitVersion.length === 3
        ? `Fishnet: ${splitVersion[1]}`
        : isLocalPane
          ? info.engineVersion
          : `Local: ${info.engineVersion}`;
    const quality =
      info.nodesPerMove === 1_000_000
        ? i18n.site.standard
        : info.nodesPerMove === 5_000_000
          ? i18n.localAnalysis.broadcast
          : (() => {
              const efficiency = this.ctrl.ceval.engines.nodeEfficiencyVsFishnet(info.id);
              if (!efficiency || this.ctrl.ceval.rules !== 'chess') return '';
              return i18n.localAnalysis.xTimesYQuality(
                Math.round((efficiency * info.nodesPerMove) / 100_000) / 10,
                i18n.site.standard.toLocaleLowerCase(),
              );
            })();
    const clearButton = (isLocalPane || this.ctrl.study?.members.canContribute()) && (
      <button
        class="clear"
        title={isLocalPane ? i18n.study.clearLocal : i18n.study.clearPublished}
        on={{ click: isLocalPane ? this.clickClearLocal : this.clickClearPublished }}>
        {licon.X}
      </button>
    );
    return [
      !isTitlePane && this.separator(i18n.localAnalysis.currentAnalysis),
      <label>{isTitlePane ? i18n.localAnalysis.published : i18n.localAnalysis.using}:</label>,
      <p class="span-three">
        {isLocalPane ? i18n.localAnalysis.local : i18n.localAnalysis.published}
        <span class="weak">
          {engine}
          {clearButton}
        </span>
      </p>,
      quality && [<label>{i18n.localAnalysis.quality}:</label>, <p>{quality}</p>],
      <label>{i18n.localAnalysis.nodesPerMove}:</label>,
      <p>{numberFormat(info.nodesPerMove)}</p>,
    ];
  }

  private separator(label: string) {
    return (
      <div class="separator">
        <hr />
        {label}
        <hr />
      </div>
    );
  }

  private getMode(preset: Preset) {
    const ceval = this.ctrl.ceval;
    if (!(preset in this.presets)) preset = 'standard';
    const efficiency = (engine: EngineInfo) =>
      engine.nodeEfficiencyVsFishnet?.[ceval.rules === 'chess' ? 'chess' : 'variant'] ?? 0;
    const id = ceval.engines
      .supporting({ rules: ceval.rules, nonStandardMaterial: ceval.nonStandardMaterial })
      .sort((a, b) => efficiency(b) - efficiency(a))[0]?.id;
    if (preset !== 'timed' && !id) return undefined;
    const search = () =>
      preset === 'timed'
        ? { maxMovetime: 300_000, maxMultiPv: 1 }
        : { by: { nodes: this.presets[preset].nodes! }, multiPv: 1 };
    const engine =
      preset === 'timed' ? undefined : { id, threads: navigator.hardwareConcurrency, hashSize: 256 };
    return { preset, customSearch: { search, engine, canBackground: true } };
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

  private get localNpm() {
    return Number(this.ctrl.idbTree.localAnalysisNpm);
  }

  private get publishedNpm() {
    return Number(this.ctrl.publishedEvalEngine?.nodesPerMove);
  }

  private timedEngineNodeEfficiency() {
    const flavor = this.ctrl.ceval.rules === 'chess' ? 'chess' : 'variant';
    return this.ctrl.ceval.info(this.mode.customSearch)!.engine.nodeEfficiencyVsFishnet?.[flavor];
  }
}
