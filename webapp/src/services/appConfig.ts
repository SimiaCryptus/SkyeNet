import {store} from '../store';

import {setAppInfo} from '../store/slices/configSlice';
import {ThemeName} from '../types';

const LOG_PREFIX = '[AppConfig]';

const BASE_API_URL = process.env.REACT_APP_API_URL || (window.location.origin + window.location.pathname);
// Add loading state tracking
let isLoadingConfig = false;
let loadConfigPromise: Promise<any> | null = null;
const STORAGE_KEYS = {
    THEME: 'theme',
} as const;
// Type guard for theme validation
const isValidTheme = (theme: unknown): theme is ThemeName => {
    const validThemes = ['main', 'night', 'forest', 'pony', 'alien', 'sunset', 'ocean', 'cyberpunk'] as const;
    return typeof theme === 'string' && validThemes.includes(theme as ThemeName);
};
export const themeStorage = {
    /**
     * Get theme with validation and fallback
     */
    getTheme(): ThemeName {
        try {
            const savedTheme = localStorage.getItem(STORAGE_KEYS.THEME);
            if (isValidTheme(savedTheme)) {
                console.log(`${LOG_PREFIX} Retrieved theme:`, savedTheme);
                return savedTheme;
            }
            console.warn(`${LOG_PREFIX} Invalid saved theme, using default`);
            return 'main';
        } catch (error) {
            console.error(`${LOG_PREFIX} Error retrieving theme:`, error);
            return 'main';
        }
    },
    /**
     * Set theme with validation and error handling
     */
    setTheme(theme: ThemeName): boolean {
        if (!isValidTheme(theme)) {
            console.error(`${LOG_PREFIX} Invalid theme:`, theme);
            return false;
        }
        try {
            localStorage.setItem(STORAGE_KEYS.THEME, theme);
            console.log(`${LOG_PREFIX} Theme saved:`, theme);
            return true;
        } catch (error) {
            console.error(`${LOG_PREFIX} Failed to save theme:`, error);
            return false;
        }
    },
    /**
     * Clear theme setting
     */
    clearTheme(): void {
        try {
            localStorage.removeItem(STORAGE_KEYS.THEME);
            console.log(`${LOG_PREFIX} Theme setting cleared`);
        } catch (error) {
            console.error(`${LOG_PREFIX} Failed to clear theme:`, error);
        }
    }
};


// Add config cache
let cachedConfig: any = null;
const CONFIG_CACHE_KEY = 'app_config_cache';
const CONFIG_CACHE_TTL = 24 * 60 * 60 * 1000; // 24 hours
// Try to load cached config from localStorage
try {
    const cached = localStorage.getItem(CONFIG_CACHE_KEY);
    if (cached) {
        const {config, timestamp} = JSON.parse(cached);
        if (Date.now() - timestamp < CONFIG_CACHE_TTL) {
            cachedConfig = config;
            console.info(`${LOG_PREFIX} Loaded valid config from cache`);
        } else {
            localStorage.removeItem(CONFIG_CACHE_KEY);
            console.info(`${LOG_PREFIX} Removed expired config cache`);
        }
    }
} catch (error) {
    console.warn(`${LOG_PREFIX} Error loading cached config:`, error);
}

export const fetchAppConfig = async (sessionId: string) => {
    try {
        // Return cached config if available
        if (cachedConfig) {
            console.info(`${LOG_PREFIX} Using cached config`);
            return cachedConfig;
        }
        // Return existing promise if already loading
        if (loadConfigPromise) {
            console.info(`${LOG_PREFIX} Config fetch already in progress, reusing promise`);
            return loadConfigPromise;
        }
        // Set loading state
        isLoadingConfig = true;
        loadConfigPromise = (async () => {

        console.info(`${LOG_PREFIX} Fetching app config:`, {
            sessionId,
            baseUrl: BASE_API_URL
        });

        const url = new URL('./appInfo', BASE_API_URL);
        url.searchParams.append('session', sessionId);

        let response: Response;

        try {
            response = await fetch(url.toString(), {
                headers: {
                    'Accept': 'application/json, text/json',
                    'Cache-Control': 'no-cache'
                },
                credentials: 'include'
            });
        } catch (networkError) {
            console.warn(`${LOG_PREFIX} Network request failed:`, {
                error: networkError,
                url: url.toString()
            });
            // Return default config for development
            if (process.env.NODE_ENV === 'development') {
                const defaultConfig = {
                    applicationName: 'Chat App (Offline)',
                    singleInput: false,
                    stickyInput: true,
                    loadImages: true,
                    showMenubar: true
                };
                cachedConfig = defaultConfig;
                store.dispatch(setAppInfo(defaultConfig));
                // Cache the config
                localStorage.setItem(CONFIG_CACHE_KEY, JSON.stringify({
                    config: defaultConfig,
                    timestamp: Date.now()
                }));
                return defaultConfig;
            }
            return null;
        }

        if (!response.ok) {
            console.warn(`${LOG_PREFIX} API error response:`, {
                status: response.status,
                statusText: response.statusText,
                url: url.toString()
            });
            const errorText = await response.text();
            console.debug(`${LOG_PREFIX} Error response body:`, errorText);
            return null;
        }

        const contentType = response.headers.get('content-type');
        if (!contentType || (!contentType.includes('application/json') && !contentType.includes('text/json'))) {
            console.error(`${LOG_PREFIX} Invalid content type:`, {
                contentType,
                url: url.toString()
            });
            throw new Error(`Invalid content type received: ${contentType}`);
        }


        const data = await response.json();
        if (!data || typeof data !== 'object') {
            console.error(`${LOG_PREFIX} Invalid response format:`, data);
            throw new Error('Invalid response format');
        }

        console.info(`${LOG_PREFIX} Received valid config:`, data);
        // Cache the config
        cachedConfig = data;
        // Store in localStorage with timestamp
        localStorage.setItem(CONFIG_CACHE_KEY, JSON.stringify({
            config: data,
            timestamp: Date.now()
        }));

        store.dispatch(setAppInfo(data));

        return data;
        })();
        const result = await loadConfigPromise;
        isLoadingConfig = false;
        loadConfigPromise = null;
        return result;

    } catch (error) {
        console.error(`${LOG_PREFIX} Config fetch failed:`, {
            error,
            sessionId,
            url: BASE_API_URL ? `${BASE_API_URL}/appInfo` : '/appInfo',
            env: process.env.NODE_ENV
        });
        isLoadingConfig = false;
        loadConfigPromise = null;
        throw error;
    }
};