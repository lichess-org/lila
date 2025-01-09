import { wrapper } from '@build/wrapper';
import { i18nContext } from './context.js';

const ctx = i18nContext();
await wrapper(ctx);
