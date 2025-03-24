import { pubsub } from 'lib/pubsub';
import PlayCtrl from './playCtrl';

export default function keyboard(ctrl: PlayCtrl): void {
  site.mousetrap
    .bind(['left', 'k'], () => ctrl.goDiff(-1))
    .bind(['right', 'j'], () => ctrl.goDiff(1))
    .bind(['up', '0', 'home'], () => ctrl.goTo(0))
    .bind(['down', '$', 'end'], () => ctrl.goToLast())
    .bind('z', () => pubsub.emit('zen'))
    .bind('f', ctrl.flip);
}
