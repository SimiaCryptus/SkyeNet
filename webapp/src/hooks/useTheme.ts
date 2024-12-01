import React, {useCallback} from 'react';
import {useDispatch, useSelector} from 'react-redux';
import {ThemeName} from '../types';
import {setTheme} from '../store/slices/uiSlice';
import {RootState} from '../store';

export const useTheme = (initialTheme?: ThemeName): [ThemeName, (theme: ThemeName) => void] => {
    console.group('🎨 useTheme Hook');
    console.log('📥 Initialization:', {
        initialTheme,
        timestamp: new Date().toISOString()
    });

    const dispatch = useDispatch();
    const currentTheme = useSelector((state: RootState) => state.ui.theme);
    console.log('🔍 Theme from Redux:', {
        currentTheme,
        stateSnapshot: new Date().toISOString()
    });

    const updateTheme = useCallback(
        (newTheme: ThemeName) => {
            console.group('🔄 Theme Update Operation');
            console.log('⚡ Dispatching theme change:', {
                from: currentTheme,
                to: newTheme,
                timestamp: new Date().toISOString()
            });
            dispatch(setTheme(newTheme));
            localStorage.setItem('theme', newTheme);
            console.log('💾 LocalStorage updated');
            console.groupEnd();
        },
        [dispatch]
    );

    // Use initialTheme if provided and no theme is set in state
    React.useEffect(() => {
        console.group('⚡ Theme Effect');
        console.log('🔄 Effect triggered:', {
            initialTheme,
            currentTheme,
            timestamp: new Date().toISOString()
        });

        if (initialTheme && !currentTheme) {
            console.log('✨ Setting initial theme:', {
                theme: initialTheme,
                reason: 'No current theme set'
            });
            updateTheme(initialTheme);
        } else {
            console.log('ℹ️ No theme update needed');
        }
        console.groupEnd();
    }, [initialTheme, currentTheme, updateTheme]);
    console.log('📤 Hook return value:', {
        currentTheme,
        hasUpdateFunction: !!updateTheme,
        timestamp: new Date().toISOString()
    });
    console.groupEnd();


    return [currentTheme, updateTheme];
};