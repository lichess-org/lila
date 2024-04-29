export default class KeyboardChecker {
  keys: string[] = [];
  oks = 0;
  kos = 0;
  prev = '';

  press = (e: KeyboardEvent) => {
    const v = (e.target as HTMLInputElement).value;
    if (v == this.prev) return;
    this.prev = v;
    if (e.key.length == 1) this.keys.push(e.key);
    else {
      if (v == '') this.clear();
      else if (e.key == 'Enter') {
        if (v.length > 1) {
          if (v.split('').every(c => this.keys.includes(c))) this.oks++;
          else {
            this.kos++;
            if (this.kos == 9 && this.kos > this.oks) site.pubsub.emit('ab.rep', 'kbc');
          }
        }
      }
    }
  };

  clear = () => {
    this.keys = [];
  };
}
