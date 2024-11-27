import {logger} from '../utils/logger';

// Export hooks with enhanced debug logging
logger.info('Initializing hooks module');
logger.debug('Starting hooks loading process from hooks/index.ts');

export {useWebSocket} from './useWebSocket';
export {useTheme} from './useTheme';
// Log successful hook exports
logger.debug('Successfully exported useWebSocket hook');
logger.debug('Successfully exported useTheme hook');
logger.info('Hooks module initialization complete');