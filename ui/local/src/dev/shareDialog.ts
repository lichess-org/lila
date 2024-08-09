import { domDialog, alert, type Dialog } from 'common/dialog';
import { ZerofishBotInfo } from '../types';
import { BotCtrl } from '../botCtrl';
import { ShareCtrl, ShareType, AssetBlob } from './shareCtrl';
import { DevRepo } from './devRepo';

export function shareDialog(botCtrl: BotCtrl, uid?: string): Promise<ShareDialog> {
  return new ShareDialog(botCtrl, uid).show();
}

class ShareDialog {
  private dlg: Dialog;
  private repo: DevRepo;

  constructor(
    readonly botCtrl: BotCtrl,
    readonly uid?: string,
  ) {
    this.repo = botCtrl.assetDb as DevRepo;
  }

  async show(): Promise<this> {
    this.dlg = await domDialog({
      class: 'dev-view share-dialog',
      htmlText: this.bodyHtml(),
      onClose: () => {},
      actions: [{ selector: '.share', listener: this.share }],
    });
    this.refresh();
    this.dlg.showModal();
    return this;
  }

  bodyHtml = () => {
    return `<div>
      <button class="button share">share</button>

    </div>`;
  };

  share = (_: Event): void => {
    console.log('we jammin');
    if (!this.uid) return;
    const bot = this.botCtrl.bots[this.uid] as ZerofishBotInfo;
    if (!bot) return;
    this.repo.share.postBot(bot).then(success =>
      alert(success ? 'shared!' : 'failed').then(() => {
        if (success) this.dlg.close();
      }),
    );
  };
  refresh(): void {}
}
