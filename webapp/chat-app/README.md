Here's a detailed README.md for the chat application:

# Chat Application

A modern, feature-rich chat application built with React, TypeScript, and Redux Toolkit. This application supports real-time messaging, multiple themes, and a responsive design.

## Features

- Real-time messaging using WebSocket
- Multiple theme support (Main, Night, Forest, Pony, Alien)
- Redux state management
- Responsive design
- TypeScript support
- Styled-components for styling
- Configurable settings
- User authentication
- Message queuing and processing

## Technology Stack

- React 18
- TypeScript
- Redux Toolkit
- Styled Components
- WebSocket
- React Router
- Jest for testing

## Project Structure

```
  src/
  ├── components/          # Reusable UI components
  ├── hooks/              # Custom React hooks
  ├── services/           # External services (WebSocket, API)
  ├── store/              # Redux store and slices
  ├── styles/             # Global styles
  ├── themes/             # Theme definitions
  ├── types/              # TypeScript type definitions
  └── App.tsx            # Root component
```

## Components

- **Chat**: Main chat interface component
- **Layout**: Application layout wrapper
- **Modal**: Reusable modal component
- **Tabs**: Tab navigation component
- **Button**: Styled button component

## State Management

The application uses Redux Toolkit with the following slices:

- **configSlice**: Application configuration
- **messageSlice**: Chat messages and queue
- **uiSlice**: UI state management
- **userSlice**: User authentication and preferences

## Themes

The application supports multiple themes:

- Main (Light)
- Night (Dark)
- Forest
- Pony
- Alien

Each theme includes:
- Color palette
- Typography settings
- Spacing and sizing
- Border radius

## WebSocket Integration

Real-time messaging is handled through a WebSocket service with:

- Automatic reconnection
- Error handling
- Message queuing
- Connection state management

## Getting Started

1. Clone the repository:
```bash
git clone <repository-url>
```

2. Install dependencies:
```bash
cd chat-app
npm install
```

3. Start the development server:
```bash
npm start
```

4. Build for production:
```bash
npm run build
```

## Configuration

The application can be configured through the Redux store:

- Single/Multiple input mode
- Sticky input
- Image loading
- Menu bar visibility
- Application name

## Testing

Run tests using:
```bash
npm test
```

## Styling

The application uses styled-components with:

- Global styles
- Theme support
- Responsive design
- CSS-in-JS

# Getting Started with Create React App

This project was bootstrapped with [Create React App](https://github.com/facebook/create-react-app).

## Available Scripts

In the project directory, you can run:

### `npm start`

Runs the app in the development mode.\
Open [http://localhost:3000](http://localhost:3000) to view it in your browser.

The page will reload when you make changes.\
You may also see any lint errors in the console.

### `npm test`

Launches the test runner in the interactive watch mode.\
See the section about [running tests](https://facebook.github.io/create-react-app/docs/running-tests) for more information.

### `npm run build`

Builds the app for production to the `build` folder.\
It correctly bundles React in production mode and optimizes the build for the best performance.

The build is minified and the filenames include the hashes.\
Your app is ready to be deployed!

See the section about [deployment](https://facebook.github.io/create-react-app/docs/deployment) for more information.

### `npm run eject`

**Note: this is a one-way operation. Once you `eject`, you can't go back!**

If you aren't satisfied with the build tool and configuration choices, you can `eject` at any time. This command will remove the single build dependency from your project.

Instead, it will copy all the configuration files and the transitive dependencies (webpack, Babel, ESLint, etc) right into your project so you have full control over them. All of the commands except `eject` will still work, but they will point to the copied scripts so you can tweak them. At this point you're on your own.

You don't have to ever use `eject`. The curated feature set is suitable for small and middle deployments, and you shouldn't feel obligated to use this feature. However we understand that this tool wouldn't be useful if you couldn't customize it when you are ready for it.

## Learn More

You can learn more in the [Create React App documentation](https://facebook.github.io/create-react-app/docs/getting-started).

To learn React, check out the [React documentation](https://reactjs.org/).

### Code Splitting

This section has moved here: [https://facebook.github.io/create-react-app/docs/code-splitting](https://facebook.github.io/create-react-app/docs/code-splitting)

### Analyzing the Bundle Size

This section has moved here: [https://facebook.github.io/create-react-app/docs/analyzing-the-bundle-size](https://facebook.github.io/create-react-app/docs/analyzing-the-bundle-size)

### Making a Progressive Web App

This section has moved here: [https://facebook.github.io/create-react-app/docs/making-a-progressive-web-app](https://facebook.github.io/create-react-app/docs/making-a-progressive-web-app)

### Advanced Configuration

This section has moved here: [https://facebook.github.io/create-react-app/docs/advanced-configuration](https://facebook.github.io/create-react-app/docs/advanced-configuration)

### Deployment

This section has moved here: [https://facebook.github.io/create-react-app/docs/deployment](https://facebook.github.io/create-react-app/docs/deployment)

### `npm run build` fails to minify

This section has moved here: [https://facebook.github.io/create-react-app/docs/troubleshooting#npm-run-build-fails-to-minify](https://facebook.github.io/create-react-app/docs/troubleshooting#npm-run-build-fails-to-minify)
