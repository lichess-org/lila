import { wrapper } from '@build/wrapper';
import { esbuildContext } from './context.js';

const ctx = esbuildContext();
await wrapper(ctx);
