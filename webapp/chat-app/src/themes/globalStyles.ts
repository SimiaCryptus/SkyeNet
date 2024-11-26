import {createGlobalStyle} from 'styled-components';

export const GlobalStyle = createGlobalStyle`
  * {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
  }

  html, body {
    height: 100%;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
      'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
      sans-serif;
  }

  #root {
    height: 100%;
  }

  code {
    font-family: source-code-pro, Menlo, Monaco, Consolas, 'Courier New',
      monospace;
  }

  /* Console Logging Styles */
  .console-log {
      background-color: #f8f8f8;
      border: 1px solid #ddd;
      border-radius: 4px;
      padding: 10px;
      margin: 5px 0;
      font-family: source-code-pro, Menlo, Monaco, Consolas, 'Courier New', monospace;
      white-space: pre-wrap;
      word-wrap: break-word;
  }

  .console-info {
      color: #0066cc;
      border-left: 4px solid #0066cc;
  }

  .console-warn {
      color: #ff9900;
      border-left: 4px solid #ff9900;
  }

  .console-error {
      color: #cc0000;
      border-left: 4px solid #cc0000;
      background-color: #fff8f8;
  }

  .console-debug {
      color: #666666;
      border-left: 4px solid #666666;
  }

  .console-success {
      color: #008800;
      border-left: 4px solid #008800;
      background-color: #f8fff8;
  }

  button {
    cursor: pointer;
    border: none;
    background: none;
    font-family: inherit;
    
    &:disabled {
      cursor: not-allowed;
    }
  }

  input, textarea {
    font-family: inherit;
  }
`;