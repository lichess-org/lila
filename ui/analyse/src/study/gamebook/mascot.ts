export default class Mascot {

  private list = [
    'octopus',
    'parrot-head',
    'camel-head',
    'owl'
  ];

  private storage = window.lichess.storage.make('gamebook.mascot');

  current = this.storage.get() || this.list[0];

  switch = () => {
    this.current = this.list[(this.list.indexOf(this.current) + 1) % this.list.length];
    this.storage.set(this.current);
  }

  url = () => window.lichess.assetUrl(`/assets/images/mascot/${this.current}.svg`);
}
