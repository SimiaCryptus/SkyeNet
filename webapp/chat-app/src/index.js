import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
// Log application startup
console.log('Starting Chat Application...');


if (typeof document !== 'undefined') {
  console.log('Initializing React root element');
  const root = ReactDOM.createRoot(document.getElementById('root'));
  try {
  root.render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
    console.log('React application successfully rendered');
  } catch (error) {
    console.error('Failed to render React application:', error);
  }
} else {
  console.warn('Document is undefined - application may be running in a non-browser environment');
}

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals((metric) => {
  console.log('Web Vital:', {
    name: metric.name,
    value: metric.value,
    rating: metric.rating,
    delta: metric.delta
  });
});