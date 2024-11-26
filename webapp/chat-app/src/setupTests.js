// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom';
// Store original console methods
const originalConsole = {
    log: console.log,
    error: console.error,
    warn: console.warn,
    info: console.info,
};
// Custom formatter for test console output
const formatTestOutput = (type, ...args) => {
    return `[TEST ${type.toUpperCase()}] ${args.join(' ')}`;
};
// Override console methods for tests
beforeAll(() => {
    console.log = (...args) => {
        originalConsole.log(formatTestOutput('log', ...args));
    };
    console.error = (...args) => {
        originalConsole.error(formatTestOutput('error', ...args));
    };
    console.warn = (...args) => {
        originalConsole.warn(formatTestOutput('warn', ...args));
    };
    console.info = (...args) => {
        originalConsole.info(formatTestOutput('info', ...args));
    };
});
// Restore original console methods after tests
afterAll(() => {
    console.log = originalConsole.log;
    console.error = originalConsole.error;
    console.warn = originalConsole.warn;
    console.info = originalConsole.info;
});