import throttle from 'common/throttle';
import { json } from 'common/xhr';

// when the simul no longer exists
const onFail = () => lichess.reload();

const post = (action: string) => (id: string) => json(`/simul/${id}/${action}`, { method: 'post' }).catch(onFail);

export default {
  ping: post('host-ping'),
  start: post('start'),
  abort: post('abort'),
  join: throttle(4000, (id: string, variant: VariantKey) => post(`join/${variant}`)(id)),
  withdraw: post('withdraw'),
  accept: (user: string) => post(`accept/${user}`),
  reject: (user: string) => post(`reject/${user}`),
};
