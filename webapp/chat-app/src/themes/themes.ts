// Define theme names
export type ThemeName = 'main' | 'night' | 'forest' | 'pony' | 'alien';

interface ThemeColors {
  primary: string;
  secondary: string;
  background: string;
  surface: string;
  text: {
    primary: string;
    secondary: string;
  };
  border: string;
  error: string;
  success: string;
  warning: string;
  info: string;
}

interface ThemeSizing {
  spacing: {
    xs: string;
    sm: string;
    md: string;
    lg: string;
    xl: string;
  };
  borderRadius: {
    sm: string;
    md: string;
    lg: string;
  };
}

interface ThemeTypography {
  fontFamily: string;
  fontSize: {
    xs: string;
    sm: string;
    md: string;
    lg: string;
    xl: string;
  };
  fontWeight: {
    regular: number;
    medium: number;
    bold: number;
  };
}

export interface ExtendedTheme {
  colors: ThemeColors;
  sizing: ThemeSizing;
  typography: ThemeTypography;
  name: string;
}

const baseTheme = {
  sizing: {
    spacing: {
      xs: '0.25rem',
      sm: '0.5rem',
      md: '1rem',
      lg: '1.5rem',
      xl: '2rem',
    },
    borderRadius: {
      sm: '0.25rem',
      md: '0.5rem',
      lg: '1rem',
    },
  },
  typography: {
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
    fontSize: {
      xs: '0.75rem',
      sm: '0.875rem',
      md: '1rem',
      lg: '1.25rem',
      xl: '1.5rem',
    },
    fontWeight: {
      regular: 400,
      medium: 500,
      bold: 700,
    },
  },
};

export const mainTheme: ExtendedTheme = {
  name: 'main',
  colors: {
    primary: '#007AFF',
    secondary: '#5856D6',
    background: '#FFFFFF',
    surface: '#F2F2F7',
    text: {
      primary: '#000000',
      secondary: '#6E6E73',
    },
    border: '#C6C6C8',
    error: '#FF3B30',
    success: '#34C759',
    warning: '#FF9500',
    info: '#5856D6',
  },
  ...baseTheme,
};

export const nightTheme: ExtendedTheme = {
  name: 'night',
  colors: {
    primary: '#0A84FF',
    secondary: '#5E5CE6',
    background: '#000000',
    surface: '#1C1C1E',
    text: {
      primary: '#FFFFFF',
      secondary: '#98989F',
    },
    border: '#38383A',
    error: '#FF453A',
    success: '#32D74B',
    warning: '#FF9F0A',
    info: '#5E5CE6',
  },
  ...baseTheme,
};

export const forestTheme: ExtendedTheme = {
  name: 'forest',
  colors: {
    primary: '#2D6A4F',
    secondary: '#40916C',
    background: '#081C15',
    surface: '#1B4332',
    text: {
      primary: '#D8F3DC',
      secondary: '#95D5B2',
    },
    border: '#2D6A4F',
    error: '#D62828',
    success: '#52B788',
    warning: '#F77F00',
    info: '#4895EF',
  },
  ...baseTheme,
};

export const ponyTheme: ExtendedTheme = {
  name: 'pony',
  colors: {
    primary: '#FF69B4',
    secondary: '#FFB6C1',
    background: '#FFF0F5',
    surface: '#FFE4E1',
    text: {
      primary: '#DB7093',
      secondary: '#C71585',
    },
    border: '#FFB6C1',
    error: '#FF1493',
    success: '#FF69B4',
    warning: '#FFB6C1',
    info: '#DB7093',
  },
  ...baseTheme,
};

export const alienTheme: ExtendedTheme = {
  name: 'alien',
  colors: {
    primary: '#39FF14',
    secondary: '#00FF00',
    background: '#0A0A0A',
    surface: '#1A1A1A',
    text: {
      primary: '#39FF14',
      secondary: '#00FF00',
    },
    border: '#008000',
    error: '#FF0000',
    success: '#39FF14',
    warning: '#FFFF00',
    info: '#00FFFF',
  },
  ...baseTheme,
};

export const themes = {
  main: mainTheme,
  night: nightTheme,
  forest: forestTheme,
  pony: ponyTheme,
  alien: alienTheme,
};
