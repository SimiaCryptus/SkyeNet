import {logger} from '../utils/logger';

// Export hooks with debug logging
logger.debug('Loading hooks from hooks/index.ts');

export {useWebSocket} from './useWebSocket';
logger.debug('Finished loading hooks');