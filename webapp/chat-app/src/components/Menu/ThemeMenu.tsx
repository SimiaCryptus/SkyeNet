import React from 'react';
import styled from 'styled-components';
import {useTheme} from '../../hooks/useTheme';
import {themes} from '../../themes/themes';

const LOG_PREFIX = '[ThemeMenu Component]';
const logWithPrefix = (message: string, ...args: any[]) => {
    console.log(`${LOG_PREFIX} ${message}`, ...args);
};
const logDebug = (message: string, ...args: any[]) => {
    if (process.env.NODE_ENV === 'development') {
        logWithPrefix(`[DEBUG] ${message}`, ...args);
    }
};

const ThemeMenuContainer = styled.div`
    position: relative;
    display: inline-block;
`;

const ThemeButton = styled.button`
    padding: ${({theme}) => theme.sizing.spacing.sm};
    color: ${({theme}) => theme.colors.text.primary};
    background: ${({theme}) => theme.colors.surface};
    border: 1px solid ${({theme}) => theme.colors.border};
    border-radius: ${({theme}) => theme.sizing.borderRadius.sm};
    transition: all 0.2s ease-in-out;

    &:hover {
        background: ${({theme}) => theme.colors.primary};
        color: ${({theme}) => theme.colors.background};
        transform: translateY(-1px);
    }
`;

const ThemeList = styled.div`
    position: absolute;
    top: 100%;
    right: 0;
    background: ${({theme}) => theme.colors.surface};
    border: 1px solid ${({theme}) => theme.colors.border};
    border-radius: ${({theme}) => theme.sizing.borderRadius.sm};
    padding: ${({theme}) => theme.sizing.spacing.xs};
    z-index: 10;
    min-width: 150px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
`;

const ThemeOption = styled.button`
    width: 100%;
    padding: ${({theme}) => theme.sizing.spacing.sm};
    text-align: left;
    color: ${({theme}) => theme.colors.text.primary};
    background: none;
    border: none;
    border-radius: ${({theme}) => theme.sizing.borderRadius.sm};

    &:hover {
        background: ${({theme}) => theme.colors.primary};
        color: ${({theme}) => theme.colors.background};
    }
`;

export const ThemeMenu: React.FC = () => {
    const [currentTheme, setTheme] = useTheme();
    const [isOpen, setIsOpen] = React.useState(false);
    const [isLoading, setIsLoading] = React.useState(false);

    React.useEffect(() => {
        logDebug('Theme changed:', {
            theme: currentTheme,
            timestamp: new Date().toISOString()
        });
    }, [currentTheme]);


    const handleThemeChange = async (themeName: keyof typeof themes) => {
        logDebug('Theme change initiated', {
            from: currentTheme,
            to: themeName,
            timestamp: new Date().toISOString()
        });

        setIsLoading(true);
        setTheme(themeName);
        setIsOpen(false);
        // Add small delay to allow theme to load
        await new Promise(resolve => setTimeout(resolve, 300));
        setIsLoading(false);
        logDebug('Theme change completed', {
            theme: themeName,
            loadTime: '300ms',
            timestamp: new Date().toISOString()
        });
    };

    const handleMenuToggle = () => {
        logDebug('Menu state changing', {
            action: !isOpen ? 'opening' : 'closing',
            currentTheme,
            timestamp: new Date().toISOString()
        });
        setIsOpen(!isOpen);
    };


    return (
        <ThemeMenuContainer>
            <ThemeButton onClick={handleMenuToggle}>
                Theme: {currentTheme}
            </ThemeButton>
            {isOpen && (
                <ThemeList>
                    {Object.keys(themes).map((themeName) => {
                        logDebug('Rendering theme option', {
                            theme: themeName,
                            isCurrentTheme: themeName === currentTheme
                        });
                        return (
                            <ThemeOption
                                key={themeName}
                                onClick={() => handleThemeChange(themeName as keyof typeof themes)}
                            >
                                {themeName}
                            </ThemeOption>
                        );
                    })}
                </ThemeList>
            )}
        </ThemeMenuContainer>
    );
};