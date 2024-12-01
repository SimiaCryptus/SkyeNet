import {themes} from './themes';
// Enhanced theme logging
const themeCount = Object.keys(themes).length;
console.group('Theme System Initialization');
console.log(`📚 Loaded ${themeCount} themes:`, Object.keys(themes));
console.table(Object.entries(themes).map(([name, theme]) => ({
    name,
    primaryColor: theme.colors?.primary || 'not set',
    background: theme.colors?.background || 'not set',
    type: theme.name || 'not set'
})));
console.groupEnd();


export {themes};
export type {ThemeName} from './themes';