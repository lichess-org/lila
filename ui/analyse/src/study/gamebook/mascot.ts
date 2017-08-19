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
    const newIndex = this.list.indexOf(this.current) + 1;
    this.current = this.list[newIndex % this.list.length];
    this.storage.set(this.current);
  }
}
