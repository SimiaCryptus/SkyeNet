

// Export hooks with enhanced debug logging
console.info('Initializing hooks module');
console.debug('Starting hooks loading process from hooks/index.ts');

export {useWebSocket} from './useWebSocket';
export {useTheme} from './useTheme';
// Log successful hook exports
console.debug('Successfully exported useWebSocket hook');
console.debug('Successfully exported useTheme hook');
console.info('Hooks module initialization complete');