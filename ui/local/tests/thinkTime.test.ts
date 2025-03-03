import { describe, /*expect,*/ test } from 'vitest';
import { makeMovetimeFunction } from '../src/movetime';

describe('think times', () => {
  test('15+0', () => {
    for (let tc of [0]) {
      console.log(tc);
      for (let r = 600; r <= 2400; r += 300) {
        console.log('rating:', r);
        const f = makeMovetimeFunction(tc, 2, r);
        console.log(' ', Array.from({ length: 40 }, (_, i) => `${i}: ${f(i).toFixed(2)}`).join(', '));
        console.log(f(120));
      }
    }
    console.log('ha');
  });
});
