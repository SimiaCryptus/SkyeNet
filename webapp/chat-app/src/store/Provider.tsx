import React, {useEffect} from 'react';
import {Provider} from 'react-redux';
import {store} from './index';

interface StoreProviderProps {
    children: React.ReactNode;
}

const LOG_PREFIX = '[StoreProvider]';


export const StoreProvider: React.FC<StoreProviderProps> = ({children}) => {
    useEffect(() => {
        console.group(`${LOG_PREFIX} Lifecycle`);
        console.log('🟢 Component Mounted');
        console.debug('Initial Store State:', store.getState());
        console.groupEnd();

        return () => {
            console.group(`${LOG_PREFIX} Lifecycle`);
            console.log('🔴 Component Unmounted');
            console.debug('Final Store State:', store.getState());
            console.groupEnd();
        };
    }, []);
    console.log(`${LOG_PREFIX} 🔄 Rendering`);

    return <Provider store={store}>{children}</Provider>;
};

// Log store initialization
console.group(`${LOG_PREFIX} Initialization`);
console.log('📦 Store Module Initialized');
console.debug('Store Instance:', store);
console.debug('Initial State:', store.getState());
console.groupEnd();


export default StoreProvider;