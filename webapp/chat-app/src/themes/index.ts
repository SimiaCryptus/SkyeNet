import {themes} from './themes';
// Log available themes on import
console.log('Available themes:', Object.keys(themes));


export {themes};
export type {ThemeName} from './themes';
export const mainTheme = themes.main;
console.log('Main theme loaded:', themes.main.name);
export const nightTheme = themes.night;
console.log('Night theme loaded:', themes.night.name);

// Additional themes will be added here
console.log('Theme system initialized');