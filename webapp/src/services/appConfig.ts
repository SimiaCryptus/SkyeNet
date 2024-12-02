import {store} from '../store';

import {setAppInfo} from '../store/slices/configSlice';
const LOG_PREFIX = '[AppConfig]';

const BASE_API_URL = process.env.REACT_APP_API_URL || window.location.origin;


export const fetchAppConfig = async (sessionId: string) => {
    try {
        console.info(`${LOG_PREFIX} Fetching app config:`, {
            sessionId,
            baseUrl: BASE_API_URL
        });

        const url = new URL('/appInfo', BASE_API_URL);
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
                store.dispatch(setAppInfo(defaultConfig));
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

        store.dispatch(setAppInfo(data));

        return data;
    } catch (error) {
        console.error(`${LOG_PREFIX} Config fetch failed:`, {
            error,
            sessionId,
            url: BASE_API_URL ? `${BASE_API_URL}/appInfo` : '/appInfo',
            env: process.env.NODE_ENV
        });
        throw error;
    }
};