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

export const applyMenubarConfig = (showMenubar: boolean) => {
  if (showMenubar === false) {
    const elements = {
      menubar: document.getElementById('toolbar'),
      namebar: document.getElementById('namebar'),
      mainInput: document.getElementById('main-input'),
      session: document.getElementById('session')
    };

    if (elements.menubar) elements.menubar.style.display = 'none';
    if (elements.namebar) elements.namebar.style.display = 'none';
    
    if (elements.mainInput) {
      elements.mainInput.style.top = '0px';
    }
    
    if (elements.session) {
      elements.session.style.top = '0px';
      elements.session.style.width = '100%';
      elements.session.style.position = 'absolute';
    }
    
    logger.info('Applied menubar config: hidden');
  }
};