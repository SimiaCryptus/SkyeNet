import {render, screen} from '@testing-library/react';
import App from './App';

console.log('🚀 Starting App component test suite...');
beforeAll(() => {
    console.log('📋 Initializing App component test environment');
});


beforeEach(() => {
    console.log('▶️ Starting new test...');
});

afterEach(() => {
    console.log('⏹️ Test finished');
});

test('renders learn react link', () => {
    console.group('🧪 Test: renders learn react link');
    console.log('⚙️ Rendering App component...');
    render(<App/>);
    console.log('🔍 Searching for "learn react" text...');
    const linkElement = screen.getByText(/learn react/i);
    console.log('✅ Verifying element is in document...');
    expect(linkElement).toBeInTheDocument();
    console.log('✨ Test completed successfully');
    console.groupEnd();
});
afterAll(() => {
    console.log('🏁 All App component tests completed');
    console.log('📊 Test suite finished');
});