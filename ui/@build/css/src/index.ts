import { wrapper } from '@build/wrapper';
import { sassContext } from './context.js';

const ctx = sassContext();
await wrapper(ctx);
