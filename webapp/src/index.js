import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
// Application initialization timestamp
const startTime = performance.now();
// Configure console styling
const logStyles = {
  startup: 'color: #4CAF50; font-weight: bold',
  error: 'color: #f44336; font-weight: bold',
  warning: 'color: #ff9800; font-weight: bold',
  info: 'color: #2196f3; font-weight: bold'
};
console.log('%c[Chat App] Starting application...', logStyles.startup);


if (typeof document !== 'undefined') {
  console.log('%c[Chat App] Initializing React root element...', logStyles.info);
  const root = ReactDOM.createRoot(document.getElementById('root'));
  try {
  root.render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
    const renderTime = (performance.now() - startTime).toFixed(2);
    console.log(
      '%c[Chat App] Application rendered successfully in %cms',
      logStyles.startup,
      renderTime
    );
  } catch (error) {
    console.log(
      '%c[Chat App] Failed to render application:',
      logStyles.error,
      '\nError:', error,
      '\nStack:', error.stack
    );
  }
} else {
  console.log(
    '%c[Chat App] Document is undefined - application may be running in a non-browser environment',
    logStyles.warning
  );
}

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals((metric) => {
  const metricColor = metric.rating === 'good' ? logStyles.startup : 
                      metric.rating === 'needs-improvement' ? logStyles.warning : 
                      logStyles.error;
  console.log(
    `%c[Web Vital] ${metric.name}:`,
    metricColor,
    `\nValue: ${metric.value.toFixed(2)}`,
    `\nRating: ${metric.rating}`,
    `\nDelta: ${metric.delta?.toFixed(2) || 'N/A'}`
  );
});