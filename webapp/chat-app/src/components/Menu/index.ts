// Logging utility for menu components
const logMenuComponent = (componentName: string) => {
    console.log(`[Menu System] Component Loaded: ${componentName} at ${new Date().toISOString()}`);
    if (process.env.NODE_ENV === 'development') {
        console.debug(`[Menu Debug] ${componentName} initialization details:`, {
            timestamp: Date.now(),
            environment: process.env.NODE_ENV
        });
    }
};

// Log when menu components are imported
logMenuComponent('ThemeMenu');
logMenuComponent('WebSocketMenu');

export {ThemeMenu} from './ThemeMenu';
export {WebSocketMenu} from './WebSocketMenu';