import {Provider} from 'react-redux';
import React from 'react';
import {createRoot} from 'react-dom/client';
import App from './App';
import {store} from './store';
import './index.css';

const rootElement = document.getElementById('root');
if (!rootElement) throw new Error('Failed to find the root element');
const root = createRoot(rootElement);

root.render(
  <Provider store={store}>
    <App />
  </Provider>
);