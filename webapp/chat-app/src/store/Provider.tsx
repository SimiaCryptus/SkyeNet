import React, {useEffect} from 'react';
import {Provider} from 'react-redux';
import {store} from './index';

interface StoreProviderProps {
    children: React.ReactNode;
}

export const StoreProvider: React.FC<StoreProviderProps> = ({children}) => {
    useEffect(() => {
        console.log('[StoreProvider] Mounted');
        return () => {
            console.log('[StoreProvider] Unmounted');
        };
    }, []);
    console.log('[StoreProvider] Rendering');

    return <Provider store={store}>{children}</Provider>;
};
// Log store initialization
console.log('[StoreProvider] Module initialized, store:', store);


export default StoreProvider;