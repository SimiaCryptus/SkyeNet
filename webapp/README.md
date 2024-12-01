Here's a detailed README.md for the chat application:

# Chat Application

A modern, feature-rich chat application built with React, TypeScript, and Redux Toolkit. This application provides real-time messaging, theme customization, and
a responsive design with enterprise-grade features.

## Key Features

### Real-Time Communication

* WebSocket-based messaging with automatic reconnection
* Message versioning and deduplication
* HTML content sanitization
* Configurable retry attempts and timeouts

### Rich UI Features

* Multiple theme options (Main, Night, Forest, Pony, Alien)
* Responsive layout with styled-components
* Modal system for dialogs
* Tab-based content organization

### Developer Experience

* TypeScript type safety
* Redux DevTools integration
* Hot module replacement

## Features

## Technology Stack

### Core

* React 18
* TypeScript 4.9
* Redux Toolkit
* Styled Components

### UI Components

* Font Awesome icons
* QR Code generator

### Security & Data

* WebSocket protocol
* Local storage persistence

## Project Structure

```
  src/
  ├── components/          # Reusable UI components
  │   ├── Menu/           # Navigation menu components
  │   ├── Modal/          # Modal dialog components
  │   └── MessageList/    # Message display components
  ├── hooks/              # Custom React hooks
  ├── services/           # External services (WebSocket, API)
  ├── store/              # Redux store and slices
  │   └── slices/         # Redux slice definitions
  ├── styles/             # Global styles
  ├── themes/             # Theme definitions
  ├── utils/              # Utility functions
  ├── types/              # TypeScript type definitions
  └── App.tsx            # Root component
```

## Key Components

### Menu System

* Main navigation menu with dropdowns
* Theme selector with live preview
* WebSocket configuration interface
* Session management controls

### Message Components

* Real-time message list with automatic updates
* Rich text input with submit handling
* Message action buttons (run, regenerate, cancel)
* HTML content rendering with sanitization

## State Management

The application uses Redux Toolkit with the following slices:

* **Config**: Application configuration state
* **Messages**: Chat message handling
* **UI**: Theme and modal management
* **User**: Authentication and preferences

Each slice includes:

* Persistence where appropriate

## Themes

The application supports multiple themes:

* **Main**: Light theme with blue accents
* **Night**: Dark theme with blue highlights
* **Forest**: Dark green nature-inspired theme
* **Pony**: Pink playful theme
* **Alien**: High contrast green/black theme

Each theme includes:

* Consistent color palette
* Typography settings
* Spacing/sizing variables

## WebSocket Integration

Real-time messaging is handled through a WebSocket service with:

* Automatic reconnection
* Message queueing
* Connection state management

## Security Features

* Content Security Policy
* XSS prevention

## Development Tools

* Redux DevTools integration
* React Developer Tools support

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

## Environment Variables

Create a `.env` file with:

```
REACT_APP_WS_URL=ws://localhost:8083
REACT_APP_API_URL=http://localhost:3000
REACT_APP_ENV=development
```

## Configuration

The application can be configured through the Redux store:

* WebSocket settings
* Logging preferences
* Theme configuration

## Testing

### Unit Tests

```bash
npm test
```

### End-to-End Tests

```bash
npm run test:e2e
```

### Type Checking

```bash
npm run type-check
```

## Styling

The application uses styled-components with:

* CSS animations
* Flexbox layouts
* CSS variables

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

If you aren't satisfied with the build tool and configuration choices, you can `eject` at any time. This command will remove the single build dependency from
your project.

Instead, it will copy all the configuration files and the transitive dependencies (webpack, Babel, ESLint, etc) right into your project so you have full control
over them. All of the commands except `eject` will still work, but they will point to the copied scripts so you can tweak them. At this point you're on your
own.

You don't have to ever use `eject`. The curated feature set is suitable for small and middle deployments, and you shouldn't feel obligated to use this feature.
However we understand that this tool wouldn't be useful if you couldn't customize it when you are ready for it.

## Learn More

You can learn more in the [Create React App documentation](https://facebook.github.io/create-react-app/docs/getting-started).

To learn React, check out the [React documentation](https://reactjs.org/).

### Code Splitting

This section has moved here: [https://facebook.github.io/create-react-app/docs/code-splitting](https://facebook.github.io/create-react-app/docs/code-splitting)

### Analyzing the Bundle Size

This section has moved
here: [https://facebook.github.io/create-react-app/docs/analyzing-the-bundle-size](https://facebook.github.io/create-react-app/docs/analyzing-the-bundle-size)

### Making a Progressive Web App

This section has moved
here: [https://facebook.github.io/create-react-app/docs/making-a-progressive-web-app](https://facebook.github.io/create-react-app/docs/making-a-progressive-web-app)

### Advanced Configuration

This section has moved
here: [https://facebook.github.io/create-react-app/docs/advanced-configuration](https://facebook.github.io/create-react-app/docs/advanced-configuration)

### Deployment

This section has moved here: [https://facebook.github.io/create-react-app/docs/deployment](https://facebook.github.io/create-react-app/docs/deployment)

### `npm run build` fails to minify

This section has moved
here: [https://facebook.github.io/create-react-app/docs/troubleshooting#npm-run-build-fails-to-minify](https://facebook.github.io/create-react-app/docs/troubleshooting#npm-run-build-fails-to-minify)