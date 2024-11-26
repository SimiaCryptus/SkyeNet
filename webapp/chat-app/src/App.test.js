import {render, screen} from '@testing-library/react';
import App from './App';

console.log('Starting App component tests...');


test('renders learn react link', () => {
    console.log('Testing: Rendering App component');
  render(<App />);
    console.log('Testing: Searching for "learn react" text');
  const linkElement = screen.getByText(/learn react/i);
    console.log('Testing: Verifying element is in document');
  expect(linkElement).toBeInTheDocument();
    console.log('Test completed successfully');
});
afterAll(() => {
    console.log('All App component tests completed');
});