import type { ChartGame, AcplChart } from 'chart';

import { myUserId } from 'lib';
import type { CustomCeval } from 'lib/ceval/types';
import { numberFormat } from 'lib/i18n';
import { licon } from 'lib/licon';
import { log } from 'lib/permalog';
import { pubsub } from 'lib/pubsub';
import type { TreeNodeLite } from 'lib/tree/types';
import { type Dialog, domDialog, spinnerHtml, confirm, alert } from 'lib/view';

import type AnalyseCtrl from '../ctrl';
import { LocalAnalysisEngine, uploadAnalysis, canUploadAnalysis } from './localAnalysisEngine';

type Preset = 'standard' | 'broadcast' | 'custom';

export function localAnalysisDialog(ctrl: AnalyseCtrl): Promise<void> {
  return new LocalAnalysisDialog(ctrl).show();
}

class LocalAnalysisDialog {
  engine: LocalAnalysisEngine;
  dlg: Dialog;
  chart: AcplChart;
  storageKey = `analyse.local.preset.${myUserId() ?? 'anon'}`;
  mode: { preset: Preset; custom: CustomCeval };
  presets: Record<Preset, { label: string; title: string; nodes?: number }>;
  justPublished = false;

  constructor(readonly ctrl: AnalyseCtrl) {
    this.engine = new LocalAnalysisEngine(ctrl, this.updateEngineStatus, this.updateChartData);
    this.presets = {
      standard: { label: i18n.site.standard, title: i18n.localAnalysis.standardQuality, nodes: 1_000_000 },
      broadcast: {
        label: i18n.localAnalysis.broadcast,
        title: i18n.localAnalysis.broadcastQuality,
        nodes: 5_000_000,
      },
      custom: {
        label: i18n.localAnalysis.custom,
        title: i18n.localAnalysis.customQualityXSeconds(ctrl.ceval.info().threads < 5 ? 4 : 2),
      },
    };
    this.selectPreset();
    pubsub.on('analysis.server.progress', this.updateView);
  }

  async show() {
    this.dlg = await domDialog({
      class: 'local-analysis-dialog',
      css: [{ hashed: 'analyse.local-dialog' }],
      htmlText: $html`
        <div class="preset-tabs">
          <label>Quality:</label>
          <button class="preset-tab" data-preset="standard">${this.presets.standard.label}</button>
          <button class="preset-tab" data-preset="broadcast">${this.presets.broadcast.label}</button>
          <button class="preset-tab" data-preset="custom">${this.presets.custom.label}</button>
        </div>
        <div class="main-content">
          <div class="preset-infos">${this.presetInfosHtml()}</div>
          <div class="chart-container none"><canvas class="chart"/></div>
          <div class="working none">
            ${spinnerHtml}
            <span>${i18n.localAnalysis.keepThisBrowserTabActive}</span>
          </div>
        </div>
        <span class="footer">
          <button class="button button-empty button-red cancel-btn none">${i18n.site.cancel}</button>
          <button class="button button-empty button-clas publish-btn none">${
            i18n.localAnalysis.publish
          }</button>
          <p class="status"></p>
          <button class="button analyse-btn">${i18n.localAnalysis.analyse}</button>
          <button class="button ok-btn none">${i18n.site.ok}</button>
        </span>`,
      modal: true,
      onClose: this.onClose,
      actions: [
        { selector: '.preset-tabs', listener: this.clickPreset },
        { selector: '.ok-btn', result: 'ok' },
        { selector: '.cancel-btn', result: 'cancel' },
        { selector: '.publish-btn', listener: this.clickPublish },
        { selector: '.analyse-btn', listener: this.clickAnalyse },
        { selector: '.engine-note', listener: this.clickEngineInfo },
        { selector: '.clear-local', listener: this.clickClearLocal },
      ],
    });
    this.chart = await site.asset
      .loadEsm<ChartGame>('chart.game')
      .then(chart => chart.acpl(this.el<HTMLCanvasElement>('.chart')!, this.ctrl.data, this.engine.nodes));
    this.updateView();
    this.dlg.show();
  }

  onClose = () => {
    this.engine.stop();
    pubsub.off('analysis.server.progress', this.updateView);
  };

  updateView = () => {
    if (!this.el('.preset-infos')?.classList.contains('hidden')) {
      this.els('.preset-tab').forEach(btn => btn.classList.remove('active'));
      this.el(`.preset-tab[data-preset="${this.mode.preset}"]`)?.classList.add('active');
      this.els('.preset-info').forEach(div => div.classList.add('none'));
      this.el(`.preset-info[data-preset="${this.mode.preset}"]`)?.classList.remove('none');
    }
    if (this.canUpload.showButton && this.ctrl.idbTree.localAnalysisIsBetter) {
      this.status(i18n.localAnalysis.youCanPublish);
      this.showButtons({ publish: true });
    } else {
      this.status('');
      this.showButtons({ publish: false });
    }
    for (const [pre, info] of Object.entries(this.presets)) {
      this.el(`.preset-tab[data-preset="${pre}"]`)?.classList.toggle(
        'checked',
        Number(info.nodes) <=
          (this.localNpm && this.serverNpm
            ? Math.max(this.localNpm, this.serverNpm)
            : this.localNpm || this.serverNpm),
      );
    }
    if (this.serverNpm >= Number(this.presets[this.mode.preset].nodes) && !this.justPublished) {
      this.status(i18n.localAnalysis.serverAlreadyHas);
    }
  };

  clickAnalyse = async (): Promise<void> => {
    this.els('.preset-tab:not(.active)')?.forEach(el => el.classList.add('none'));
    this.showButtons({ publish: false, cancel: true, analyse: false });
    this.el('.preset-infos')?.classList.add('hidden');
    this.el('.chart-container')?.classList.remove('none');
    this.el('.working')?.classList.remove('none');
    const then = performance.now();
    try {
      const result = await this.engine.analyse(this.mode.custom);
      await this.ctrl.idbTree.saveAnalysis(result);
      this.status(i18n.localAnalysis.doneInX(((performance.now() - then) / 1000).toFixed(1)));
      this.showButtons({ ok: true, publish: this.canUpload.showButton, cancel: false });
      this.ctrl.mergeAnalysisData(result.localAnalysis, false);
    } catch (e) {
      this.el('.working')?.classList.add('none');
      if (e !== 'cancelled') {
        log(e);
        await alert(String(e));
      }
    }
    this.el('.working')?.classList.add('none');
    this.updateView();
    this.ctrl.redraw();
  };

  clickPublish = async () => {
    if (this.canUpload.whyNot) {
      return await alert(this.canUpload.whyNot);
    }
    if (this.serverNpm && this.ctrl.study && !this.ctrl.study.canMergeAnalysisCleanly()) {
      if (!(await confirm(i18n.localAnalysis.whenUpgradingOldChapters, i18n.localAnalysis.publish))) return;
    }

    const result = await uploadAnalysis(await this.ctrl.idbTree.serverDocument());

    if (result.status === 'locked') {
      await alert(i18n.localAnalysis.serverAnalysisInProgress);
    } else if (result.status === 'error') {
      if (result.errorText) log('analysis upload error', result.errorText);
      this.status(i18n.localAnalysis.analysisUploadFailed);
    } else if (result.status === 'conflict') {
      const useTheirs = await confirm(
        i18n.localAnalysis.looksLikeASimilar,
        i18n.localAnalysis.useTheirs,
        i18n.localAnalysis.keepMine,
      );
      if (useTheirs) {
        await this.ctrl.idbTree.clear('analysis');
        site.reload();
      } else this.dlg.close();
    } else {
      this.justPublished = true;
      await this.ctrl.idbTree.clear('analysis');
      this.updateView();
      this.showButtons({ ok: true, analyse: false, publish: false });
      this.status(i18n.site.success);
    }
  };

  clickPreset = (e: Event) => {
    if (!(e.target instanceof HTMLElement)) return;

    const preset = e.target.dataset.preset as Preset;
    localStorage.setItem(this.storageKey, preset);
    this.selectPreset(preset);
    this.updateView();
  };

  clickEngineInfo = () => {
    const info = this.ctrl.ceval.engines.getEngine({
      variant: this.ctrl.data.game.variant.key,
      capability: 'staticAnalysis',
    });
    alert(
      info
        ? i18n.localAnalysis.youMustUseX(info.short ?? info.name)
        : i18n.localAnalysis.yourDeviceDoesNotSupport,
    );
  };

  clickClearLocal = async () => {
    if (await confirm(i18n.localAnalysis.clearLocalAnalysis)) {
      await this.ctrl.idbTree.clear('analysis');
      site.reload();
    }
  };

  selectPreset(preset?: Preset) {
    if (!preset) {
      const nodesToBeat = Math.max(Number(this.localNpm), Number(this.serverNpm));
      preset = localStorage.getItem(this.storageKey) as Preset;
      if (!(preset in this.presets)) preset = 'standard';
      if (Number(this.presets[preset].nodes) < nodesToBeat) {
        preset = nodesToBeat < this.presets.broadcast.nodes! ? 'broadcast' : 'custom';
      }
    }
    this.mode = this.getMode(preset);
  }

  updateEngineStatus = (nodeIndex: number, totalNodes: number, nodesPerMove: number) => {
    let html =
      nodeIndex === 0
        ? i18n.localAnalysis.startingPosition
        : i18n.localAnalysis.moveXOfY(nodeIndex, totalNodes - 1);

    if (this.mode.preset === 'custom' && isFinite(nodesPerMove)) {
      for (const fasterThan of [this.presets.broadcast, this.presets.standard]) {
        const presetNodes = fasterThan.nodes!;
        if (nodesPerMove <= presetNodes) continue;
        const multiplier =
          nodesPerMove / presetNodes < 5
            ? Math.round(10 * (nodesPerMove / presetNodes)) / 10
            : Math.round(nodesPerMove / presetNodes);
        html += `<br>(${i18n.localAnalysis.xTimesYQuality(
          multiplier,
          fasterThan.label.toLocaleLowerCase(),
        )})`;
        break;
      }
    }
    this.status(html);
  };

  status(html: string) {
    this.el('.status')!.innerHTML = html ?? '';
  }

  presetInfosHtml() {
    return ['standard', 'broadcast', 'custom'].map(this.presetInfoHtml).join('');
  }

  presetInfoHtml = (preset: Preset) => {
    const param = (label: string, value: string, postfix?: string) =>
      `<label>${label}:</label><p ${postfix !== undefined ? 'class="row-val"' : ''}>${value} ${
        postfix ? postfix : ''
      }</p>`;
    const info = this.ctrl.ceval.info(this.getMode(preset).custom);

    const projectedQuality = () => {
      if (!('movetime' in info.search.by) || !info.engine.capabilities?.includes('staticAnalysis')) return '';

      const balancedCores = info.threads < 10 ? info.threads : 10 + (info.threads - 10) / 1.5;
      let multiplier = Math.round((balancedCores * info.search.by.movetime) / 300) / 10;
      if (multiplier > 10) multiplier = Math.round(multiplier);
      return param(
        i18n.localAnalysis.projected,
        i18n.localAnalysis.xTimesYQuality(multiplier, i18n.site.standard.toLocaleLowerCase()),
      );
    };

    const searchParam =
      'movetime' in info.search.by
        ? param(i18n.site.time, i18n.site.nbSeconds(Math.round(info.search.by.movetime / 1000)))
        : 'nodes' in info.search.by
          ? param(i18n.localAnalysis.nodes, numberFormat(info.search.by.nodes))
          : '';
    const engineNameParam = param(
      i18n.localAnalysis.willUse,
      info.engine?.short ?? info.engine?.name ?? '',
      info.engine?.capabilities?.includes('staticAnalysis')
        ? ''
        : `<a class="engine-note">(${i18n.localAnalysis.cannotPublish})</a>`,
    );

    return $html`
      <div class="preset-info" data-preset="${preset}">
        <span>${this.presets[preset].title}</span>
        ${this.currentAnalysisHtml()}
        ${this.separator(i18n.localAnalysis.XAnalysis(this.presets[preset].label))}
        ${engineNameParam}
        ${searchParam}
        ${projectedQuality()}
      </div>`;
  };

  currentAnalysisHtml() {
    const info = this.ctrl.idbTree.localAnalysisInfo ?? this.ctrl.data.analysis?.engine;
    if (!info) return '';

    const engine = this.ctrl.ceval.engines.getEngine(info);
    const npm = info.nodesPerMove;
    const quality =
      npm === 1_000_000
        ? i18n.site.standard
        : npm === 5_000_000
          ? i18n.localAnalysis.broadcast
          : i18n.localAnalysis.xTimesYQuality(
              Math.round(npm / 100_000) / 10,
              i18n.site.standard.toLocaleLowerCase(),
            );
    const provenance = this.ctrl.idbTree.hasLocalAnalysis
      ? i18n.localAnalysis.localAnalysis
      : i18n.localAnalysis.serverAnalysis;
    const clearButton = this.ctrl.idbTree.hasLocalAnalysis
      ? `<a data-icon="${licon.X}"class="clear-local"></a>`
      : '';
    return $html`
      ${this.separator(i18n.localAnalysis.currentAnalysis)}
      <label>${i18n.localAnalysis.using}:</label>
      <p class="row-val">
        ${provenance}
        <span class="note">(${engine?.short ?? engine?.name ?? 'Fishnet'}) ${clearButton}</span>
      </p>
      <label>${i18n.localAnalysis.quality}:</label>
      <p>${quality}</p>
      <label>${i18n.localAnalysis.nodes}:</label>
      <p>${numberFormat(info.nodesPerMove)}</p>`;
  }

  separator(label: string) {
    return `<div class="separator"><hr>${label}<hr></div>`;
  }

  getMode(val: Preset) {
    if (!(val in this.presets)) val = 'standard';
    const id = this.ctrl.ceval.engines.supporting(
      this.ctrl.data.game.variant.key,
      'staticAnalysis',
      'browser',
    )[0].id;
    const search = () =>
      val === 'custom' ? 60_000 : { by: { nodes: this.presets[val].nodes! }, multiPv: 1 };
    const engine =
      val !== 'custom' ? { id, threads: navigator.hardwareConcurrency, hashSize: 512 } : undefined;
    return { preset: val, custom: { search, engine, canBackground: true } };
  }

  showButtons(state: { cancel?: boolean; publish?: boolean; analyse?: boolean; ok?: boolean }) {
    Object.entries(state).forEach(([btn, vis]) => this.el(`.${btn}-btn`)?.classList.toggle('none', !vis));
  }

  updateChartData = (nodes: TreeNodeLite[]) => {
    this.chart?.updateData(this.ctrl.data, nodes);
  };

  el<T extends HTMLElement>(selector: string) {
    return this.dlg.view.querySelector<T>(selector) ?? undefined;
  }

  els<T extends HTMLElement>(selector: string) {
    return this.dlg.view.querySelectorAll<T>(selector);
  }

  get canUpload() {
    const { allowed, reason } = canUploadAnalysis(this.ctrl);
    const showButton =
      reason === 'rec' ||
      (allowed &&
        this.ctrl.idbTree.localAnalysisIsBetter &&
        (this.selectedEngine?.capabilities?.includes('staticAnalysis') ?? false) &&
        (this.mode.preset === 'custom' ||
          this.ctrl.idbTree.localAnalysisNpm === this.presets[this.mode.preset].nodes));

    return { showButton, whyNot: reason === 'rec' ? i18n.localAnalysis.turnOnRec : reason };
  }

  get selectedEngine() {
    return this.ctrl.ceval.info(this.mode.custom).engine;
  }
  get localNpm() {
    return Number(this.ctrl.idbTree.localAnalysisNpm);
  }
  get serverNpm() {
    return Number(this.ctrl.data.analysis?.engine?.nodesPerMove);
  }
}
