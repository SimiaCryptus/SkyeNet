import {store} from '../store';
import {logger} from '../utils/logger';
import {setAppInfo} from '../store/slices/configSlice';

export const fetchAppConfig = async (sessionId: string) => {
    try {
        logger.info('Fetching app config for session:', sessionId);
        const response = await fetch(`appInfo?session=${sessionId}`);

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const data = await response.json();
        logger.info('Received app config:', data);

        store.dispatch(setAppInfo(data));

        return data;
    } catch (error) {
        logger.error('Failed to fetch app config:', error);
        throw error;
    }
};
