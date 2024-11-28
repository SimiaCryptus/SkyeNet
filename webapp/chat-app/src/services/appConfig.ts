import {store} from '../store';
import {logger} from '../utils/logger';
import {setAppInfo} from '../store/slices/configSlice';
const BASE_API_URL = process.env.REACT_APP_API_URL || window.location.origin;


export const fetchAppConfig = async (sessionId: string) => {
    try {
        logger.info('Fetching app config for session:', sessionId);
        const url = new URL('/api/appInfo', BASE_API_URL);
        url.searchParams.append('session', sessionId);
        let response: Response;
        // Add error handling for failed requests
        try {
            response = await fetch(url.toString(), {
            headers: {
                'Accept': 'application/json'
            }
        });
        } catch (networkError) {
            logger.warn('Network request failed:', networkError);
            return null;
        }

        if (!response.ok) {
            logger.warn(`API returned error status: ${response.status}`);
            return null;
        }
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new Error(`Invalid content type: ${contentType}`);
        }


        const data = await response.json();
        if (!data || typeof data !== 'object') {
            throw new Error('Invalid response format');
        }
        
        logger.info('Received app config:', data);

        store.dispatch(setAppInfo(data));

        return data;
    } catch (error) {
        logger.error('Failed to fetch app config:', {
            error,
            sessionId,
            url: BASE_API_URL ? `${BASE_API_URL}/api/appInfo` : '/api/appInfo'
        });
        throw error;
    }
};