// Logging utility for menu components
const logMenuComponent = (componentName: string) => {
    console.log(`Menu Component Loaded: ${componentName}`);
};

// Log when ThemeMenu is imported
logMenuComponent('ThemeMenu');
export {ThemeMenu} from './ThemeMenu';
export {WebSocketMenu} from './WebSocketMenu';